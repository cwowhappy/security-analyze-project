"""Akshare 数据源适配器（主数据源）。"""

import random
import time
from typing import Any

import akshare as ak
import structlog

from data_collector.config import Settings
from data_collector.core.domain.company import Company
from data_collector.core.domain.data_source_error import DataSourceError, SourceRateLimitError
from data_collector.core.domain.stock import Stock
from data_collector.core.ports.data_source import DataSource, SourceHealth, SourceStatus

logger = structlog.get_logger(__name__)


class AkshareDataSource(DataSource):
    """Akshare 数据源适配器。

    使用 akshare 免费接口获取 A 股股票列表和公司详情。
    """

    def __init__(self, settings: Settings | None = None) -> None:
        self._settings = settings or Settings()
        self._call_count = 0
        self._error_count = 0
        self._last_check = ""

    @property
    def name(self) -> str:
        return "akshare"

    @property
    def priority(self) -> int:
        return 1

    def _delay(self) -> None:
        """请求间随机延迟，避免触发限流。"""
        delay = random.uniform(
            self._settings.source_request_delay_min,
            self._settings.source_request_delay_max,
        )
        time.sleep(delay)

    def _retry(self, fn, *args, **kwargs) -> Any:
        """带重试的函数调用。"""
        max_retries = self._settings.source_max_retries
        delay = self._settings.source_retry_delay
        backoff = self._settings.source_retry_backoff

        for attempt in range(max_retries):
            try:
                return fn(*args, **kwargs)
            except Exception as e:
                self._error_count += 1
                if "限流" in str(e) or "rate" in str(e).lower():
                    logger.warning("akshare 限流", attempt=attempt + 1, error=str(e))
                    raise SourceRateLimitError(self.name) from e
                if attempt < max_retries - 1:
                    logger.warning(
                        "akshare 请求失败，准备重试",
                        attempt=attempt + 1,
                        delay=delay,
                        error=str(e),
                    )
                    time.sleep(delay)
                    delay *= backoff
                else:
                    raise DataSourceError(self.name, f"请求失败: {e}") from e
        return None

    def fetch_stock_list(self) -> list[Stock]:
        """获取全量 A 股股票列表。

        组合 akshare 的多个接口获取完整信息。
        """

        logger.info("开始从 akshare 获取全量股票列表")
        self._delay()

        df = self._retry(ak.stock_info_a_code_name)
        self._call_count += 1

        if df is None or df.empty:
            logger.warning("akshare 返回空数据")
            return []

        stocks: list[Stock] = []
        for _, row in df.iterrows():
            try:
                market = str(row.get("market", "")).strip() if "market" in row else None
                stock_code = str(row.get("code", "")).strip()
                stock = Stock(
                    stock_code=stock_code,
                    name=str(row.get("name", "")).strip(),
                    ts_code=self._to_ts_code(stock_code, market),
                    full_name=str(row.get("name", "")).strip(),
                    market=market,
                    exchange=self._to_exchange(stock_code, market),
                )
                stocks.append(stock)
            except (ValueError, KeyError) as e:
                logger.debug("解析股票行失败", row=dict(row), error=str(e))
                continue

        logger.info("akshare 股票列表获取完成", count=len(stocks))
        return stocks

    def fetch_company_info(self, stock_code: str) -> Company | None:
        """获取单只股票对应的公司详情。

        Args:
            stock_code: 股票代码（如 000001）。
        """

        logger.info("开始从 akshare 获取公司详情", stock_code=stock_code)
        self._delay()

        try:
            df = self._retry(ak.stock_profile_cninfo, symbol=stock_code)
            self._call_count += 1
        except DataSourceError:
            return None

        if df is None or df.empty:
            logger.debug("akshare 未找到公司详情", stock_code=stock_code)
            return None

        row = df.iloc[0]
        try:
            from contextlib import suppress
            from datetime import datetime

            setup_date = None
            if "成立日期" in row and row["成立日期"]:
                with suppress(ValueError):
                    setup_date = datetime.strptime(str(row["成立日期"]), "%Y-%m-%d").date()

            reg_capital = None
            if "注册资本" in row and row["注册资本"]:
                try:
                    # 去除单位（万元、亿元等），保留数字
                    val = str(row["注册资本"]).replace(",", "").replace("万", "").replace("亿", "")
                    reg_capital = float(val)
                except ValueError:
                    pass

            company = Company(
                name=str(row.get("公司名称", stock_code)).strip(),
                unified_social_credit_code=str(row.get("统一社会信用代码", "")).strip() or None,
                legal_representative=str(row.get("法人代表", "")).strip() or None,
                chairman=str(row.get("董事长", "")).strip() or None,
                manager=str(row.get("总经理", "")).strip() or None,
                secretary=str(row.get("董秘", "")).strip() or None,
                reg_capital=reg_capital,
                setup_date=setup_date,
                province=str(row.get("省份", "")).strip() or None,
                city=str(row.get("城市", "")).strip() or None,
                reg_address=str(row.get("注册地址", "")).strip() or None,
                office_address=str(row.get("办公地址", "")).strip() or None,
                website=str(row.get("公司网站", "")).strip() or None,
                industry=str(row.get("行业分类", "")).strip() or None,
                main_business=str(row.get("主营业务", "")).strip() or None,
                business_scope=str(row.get("经营范围", "")).strip() or None,
                introduction=str(row.get("公司简介", "")).strip() or None,
            )
            logger.info("公司详情获取成功", stock_code=stock_code, name=company.name)
            return company
        except Exception as e:
            logger.warning("解析公司详情失败", stock_code=stock_code, error=str(e))
            return None

    def check_health(self) -> SourceHealth:
        """检查数据源健康状态。"""
        import time as time_mod


        start = time_mod.time()
        try:
            # 轻量级健康检查：获取 akshare 版本或调用简单接口
            _ = ak.stock_info_a_code_name()
            latency = (time_mod.time() - start) * 1000
            self._last_check = time_mod.strftime("%Y-%m-%dT%H:%M:%SZ", time_mod.gmtime())
            return SourceHealth(
                status=SourceStatus.HEALTHY,
                latency_ms=latency,
                error_rate=self._error_rate(),
                last_check=self._last_check,
            )
        except Exception:
            self._last_check = time_mod.strftime("%Y-%m-%dT%H:%M:%SZ", time_mod.gmtime())
            return SourceHealth(
                status=SourceStatus.UNAVAILABLE,
                latency_ms=(time_mod.time() - start) * 1000,
                error_rate=self._error_rate(),
                last_check=self._last_check,
            )

    def _error_rate(self) -> float:
        if self._call_count == 0:
            return 0.0
        return self._error_count / self._call_count

    @staticmethod
    def _to_ts_code(stock_code: str, market: str | None) -> str | None:
        """将股票代码转换为 ts_code 格式（如 000001.SZ）。"""
        code = str(stock_code).strip()
        if not code:
            return None
        if market:
            m = str(market).upper()
            if m in ("SH", "SSE"):
                return f"{code}.SH"
            if m in ("SZ", "SZSE"):
                return f"{code}.SZ"
            if m in ("BJ", "BSE"):
                return f"{code}.BJ"
        # 根据代码前缀推断交易所
        if code.startswith("6"):
            return f"{code}.SH"
        if code.startswith(("0", "3")):
            return f"{code}.SZ"
        if code.startswith(("4", "8")):
            return f"{code}.BJ"
        return None

    @staticmethod
    def _to_exchange(stock_code: str, market: str | None) -> str | None:
        """根据 market 或代码前缀推断交易所代码（SSE / SZSE / BSE）。"""
        if market:
            m = str(market).upper()
            if m in ("SH", "SSE"):
                return "SSE"
            if m in ("SZ", "SZSE"):
                return "SZSE"
            if m in ("BJ", "BSE"):
                return "BSE"
        code = str(stock_code).strip()
        if code.startswith("6"):
            return "SSE"
        if code.startswith(("0", "3")):
            return "SZSE"
        if code.startswith(("4", "8")):
            return "BSE"
        return None
