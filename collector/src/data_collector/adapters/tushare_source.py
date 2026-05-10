"""Tushare 数据源适配器（备用数据源）。"""

import random
import time
from typing import Any

import structlog
import tushare as ts

from data_collector.config import Settings
from data_collector.core.domain.company import Company
from data_collector.core.domain.data_source_error import DataSourceError, SourceRateLimitError
from data_collector.core.domain.stock import Stock
from data_collector.core.ports.data_source import DataSource, SourceHealth, SourceStatus

logger = structlog.get_logger(__name__)


class TushareDataSource(DataSource):
    """Tushare 数据源适配器。

    使用 tushare Pro 接口获取股票和公司信息，受积分限制，作为备用源。
    """

    def __init__(self, settings: Settings | None = None) -> None:
        self._settings = settings or Settings()
        self._pro = None
        self._call_count = 0
        self._error_count = 0
        self._last_check = ""

    def _get_pro(self) -> Any:
        """懒加载 tushare pro 接口。"""
        if self._pro is not None:
            return self._pro

        token = self._settings.tushare_token
        if not token:
            raise DataSourceError(self.name, "TUSHARE_TOKEN 未配置")

        ts.set_token(token)
        self._pro = ts.pro_api()
        return self._pro

    @property
    def name(self) -> str:
        return "tushare"

    @property
    def priority(self) -> int:
        return 2

    def _delay(self) -> None:
        """请求间随机延迟。"""
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
                msg = str(e)
                if "limit" in msg.lower() or "freq" in msg.lower() or "积分" in msg:
                    logger.warning("tushare 限流", attempt=attempt + 1, error=msg)
                    raise SourceRateLimitError(self.name) from e
                if attempt < max_retries - 1:
                    logger.warning(
                        "tushare 请求失败，准备重试",
                        attempt=attempt + 1,
                        delay=delay,
                        error=msg,
                    )
                    time.sleep(delay)
                    delay *= backoff
                else:
                    raise DataSourceError(self.name, f"请求失败: {e}") from e
        return None

    def fetch_stock_list(self) -> list[Stock]:
        """获取全量 A 股股票列表。"""
        logger.info("开始从 tushare 获取全量股票列表")
        self._delay()

        pro = self._get_pro()
        df = self._retry(pro.stock_basic, exchange="", list_status="L")
        self._call_count += 1

        if df is None or df.empty:
            logger.warning("tushare 返回空数据")
            return []

        stocks: list[Stock] = []
        for _, row in df.iterrows():
            try:
                from contextlib import suppress
                from datetime import datetime

                list_date = None
                if "list_date" in row and row["list_date"]:
                    with suppress(ValueError):
                        list_date = datetime.strptime(str(row["list_date"]), "%Y%m%d").date()

                stock = Stock(
                    stock_code=str(row.get("ts_code", "")).split(".")[0].strip(),
                    name=str(row.get("name", "")).strip(),
                    ts_code=str(row.get("ts_code", "")).strip(),
                    full_name=str(row.get("fullname", "")).strip() or None,
                    market=str(row.get("market", "")).strip() or None,
                    exchange=str(row.get("exchange", "")).strip() or None,
                    list_date=list_date,
                    industry=str(row.get("industry", "")).strip() or None,
                    area=str(row.get("area", "")).strip() or None,
                )
                stocks.append(stock)
            except (ValueError, KeyError) as e:
                logger.debug("解析股票行失败", row=dict(row), error=str(e))
                continue

        logger.info("tushare 股票列表获取完成", count=len(stocks))
        return stocks

    def fetch_company_info(self, stock_code: str) -> Company | None:
        """获取单只股票对应的公司详情。"""
        logger.info("开始从 tushare 获取公司详情", stock_code=stock_code)
        self._delay()

        pro = self._get_pro()
        # tushare 的 stock_company 需要 ts_code
        ts_code = self._guess_ts_code(stock_code)

        try:
            df = self._retry(pro.stock_company, ts_code=ts_code)
            self._call_count += 1
        except DataSourceError:
            return None

        if df is None or df.empty:
            logger.debug("tushare 未找到公司详情", stock_code=stock_code)
            return None

        row = df.iloc[0]
        try:
            from contextlib import suppress
            from datetime import datetime

            setup_date = None
            if "setup_date" in row and row["setup_date"]:
                with suppress(ValueError):
                    setup_date = datetime.strptime(str(row["setup_date"]), "%Y%m%d").date()

            company = Company(
                name=str(row.get("name", "")).strip(),
                unified_social_credit_code=str(row.get("credit_code", "")).strip() or None,
                legal_representative=str(row.get("legal_person", "")).strip() or None,
                chairman=str(row.get("chairman", "")).strip() or None,
                manager=str(row.get("manager", "")).strip() or None,
                secretary=str(row.get("secretary", "")).strip() or None,
                reg_capital=float(row["reg_capital"]) if "reg_capital" in row and row["reg_capital"] else None,
                setup_date=setup_date,
                province=str(row.get("province", "")).strip() or None,
                city=str(row.get("city", "")).strip() or None,
                reg_address=str(row.get("reg_address", "")).strip() or None,
                office_address=str(row.get("office", "")).strip() or None,
                website=str(row.get("website", "")).strip() or None,
                main_business=str(row.get("main_business", "")).strip() or None,
                business_scope=str(row.get("business_scope", "")).strip() or None,
                introduction=str(row.get("introduction", "")).strip() or None,
                employees=int(row["employees"]) if "employees" in row and row["employees"] else None,
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
            pro = self._get_pro()
            _ = pro.stock_basic(exchange="", list_status="L", limit=1)
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
    def _guess_ts_code(stock_code: str) -> str:
        """根据股票代码猜测 ts_code。"""
        code = str(stock_code).strip()
        if code.startswith("6"):
            return f"{code}.SH"
        if code.startswith(("0", "3")):
            return f"{code}.SZ"
        if code.startswith(("4", "8")):
            return f"{code}.BJ"
        return code
