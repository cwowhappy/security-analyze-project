"""股票基础信息 Tushare 适配器。

Tushare stock_basic 返回全量数据，本适配器在第一次 fetch 时缓存全量结果，
随后从缓存中按 stock_code 查找。
"""

from typing import Any

import structlog

from data_collector.core.config.field_mapping_config import SourceConfig

logger = structlog.get_logger(__name__)


def _init_tushare() -> object:
    """初始化 Tushare Pro API。"""
    from data_collector.config import Settings

    settings = Settings()
    token = settings.tushare_token
    if not token:
        raise RuntimeError("TUSHARE_TOKEN 未配置，无法使用 Tushare 数据源")
    import tushare as ts

    ts.set_token(token)
    return ts.pro_api()


class StockBasicTushareAdapter:
    def __init__(self) -> None:
        self._cache: dict[str, dict[str, Any]] | None = None

    def fetch(self, stock_code: str, source_config: SourceConfig) -> dict[str, Any]:
        if self._cache is None:
            self._build_cache(source_config)
        return self._cache.get(stock_code, {})

    def _build_cache(self, source_config: SourceConfig) -> None:
        pro = _init_tushare()
        fields = source_config.params.get(
            "fields",
            "ts_code,symbol,name,area,industry,fullname,market,exchange,list_date",
        )

        # 1. 拉取 stock_basic 全量
        try:
            df_basic = pro.stock_basic(exchange="", list_status="L", fields=fields)
            if df_basic is None or df_basic.empty:
                logger.warning("tushare stock_basic 返回空数据")
                self._cache = {}
                return
        except Exception as e:
            logger.error("tushare stock_basic 查询失败", error=str(e))
            self._cache = {}
            return

        # 以 symbol（股票代码）为 key 构建缓存
        cache: dict[str, dict[str, Any]] = {}
        ts_to_symbol: dict[str, str] = {}
        for _, row in df_basic.iterrows():
            symbol = str(row.get("symbol", "")).strip()
            ts_code = str(row.get("ts_code", "")).strip()
            if symbol:
                cache[symbol] = row.to_dict()
                if ts_code:
                    ts_to_symbol[ts_code] = symbol

        # 2. 拉取 daily_basic 最新交易日全量，补充 total_share / float_share
        try:
            from datetime import datetime

            today = datetime.now().strftime("%Y%m%d")
            df_trade = pro.trade_cal(exchange="SSE", start_date=today, end_date=today)
            if df_trade is not None and not df_trade.empty:
                last_trade = df_trade[df_trade["is_open"] == 1]["cal_date"].max()
            else:
                last_trade = today

            df_cap = pro.daily_basic(
                trade_date=last_trade, fields="ts_code,total_share,float_share"
            )
            if df_cap is not None and not df_cap.empty:
                for _, row in df_cap.iterrows():
                    ts_code = str(row.get("ts_code", "")).strip()
                    symbol = ts_to_symbol.get(ts_code)
                    if symbol and symbol in cache:
                        cache[symbol]["total_share"] = row.get("total_share")
                        cache[symbol]["float_share"] = row.get("float_share")
        except Exception as e:
            logger.warning("tushare daily_basic 查询失败", error=str(e))

        self._cache = cache
