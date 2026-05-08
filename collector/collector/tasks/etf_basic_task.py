import logging
from typing import List, Dict, Any

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor
from collector.models import EtfInfoEntity
from collector.tasks.base import BaseTask, TaskResult

logger = logging.getLogger(__name__)


class EtfBasicTask(BaseTask):
    """采集 ETF 基本信息任务（全量 upsert）"""

    task_name = "etf_basic"
    data_type = "etf_basic"

    def __init__(self, db: PostgresDB, source: BaseDataSource, monitor: Monitor = None):
        super().__init__(db=db, source=source, monitor=monitor)

    def run(self, **kwargs) -> int:
        """向后兼容的手动执行入口，返回处理的记录数。"""
        result = self.run_full(**kwargs)
        return result.rows

    def run_full(self, **kwargs) -> TaskResult:
        """全量采集 ETF 基本信息。"""
        logger.info("开始采集 ETF 基本信息")
        raw_list = self.source.get_etf_spot_list()
        logger.info(f"从数据源获取到 {len(raw_list)} 条 ETF 记录")

        entities = [self._parse_etf(item) for item in raw_list]
        entities = [e for e in entities if e is not None]

        if not entities:
            logger.warning("没有解析到有效的 ETF 数据")
            return TaskResult(rows=0)

        self._upsert_to_db(entities)
        logger.info(f"ETF 基本信息采集完成，共 {len(entities)} 条")
        return TaskResult(rows=len(entities))

    def run_partial(self, identifiers: List[str], **kwargs) -> TaskResult:
        """指定 ETF 代码列表采集。"""
        logger.info(f"开始采集指定 ETF 基本信息: {identifiers}")
        raw_list = self.source.get_etf_spot_list()
        id_set = set(identifiers)
        filtered = [item for item in raw_list if str(item.get("代码", "")).strip() in id_set]
        logger.info(f"从数据源获取到 {len(filtered)} 条匹配 ETF 记录")

        entities = [self._parse_etf(item) for item in filtered]
        entities = [e for e in entities if e is not None]

        if not entities:
            logger.warning("没有解析到有效的 ETF 数据")
            return TaskResult(rows=0)

        self._upsert_to_db(entities)
        logger.info(f"指定 ETF 基本信息采集完成，共 {len(entities)} 条")
        return TaskResult(rows=len(entities))

    def run_incremental(self, **kwargs) -> TaskResult:
        """增量采集（数据量小，暂按全量处理）。"""
        logger.info("ETF 基本信息增量采集暂按全量处理")
        return self.run_full(**kwargs)

    def _parse_etf(self, raw: Dict[str, Any]) -> EtfInfoEntity:
        """解析原始数据为 EtfInfoEntity"""
        try:
            etf_code = str(raw.get("代码", "")).strip()
            etf_name = str(raw.get("名称", "")).strip()
            if not etf_code or not etf_name:
                return None

            # fund_size 从 总市值 或 流通市值 取（单位：元）
            fund_size = self._parse_float(raw.get("总市值")) or self._parse_float(raw.get("流通市值"))

            return EtfInfoEntity(
                etf_code=etf_code,
                etf_name=etf_name,
                fund_size=fund_size,
                market=self._infer_market(etf_code),
            )
        except Exception as e:
            logger.debug(f"解析 ETF 数据失败: {raw}, error={e}")
            return None

    @staticmethod
    def _parse_float(val) -> float:
        if val is None:
            return None
        try:
            return float(val)
        except (ValueError, TypeError):
            return None

    @staticmethod
    def _infer_market(etf_code: str) -> str:
        """根据 ETF 代码推断市场"""
        if etf_code.startswith("5"):
            return "SH"
        if etf_code.startswith("1") or etf_code.startswith("3"):
            return "SZ"
        return "CN"

    def _upsert_to_db(self, entities: List[EtfInfoEntity]):
        """批量 upsert 到数据库"""
        sql = EtfInfoEntity.upsert_sql()
        tuples = [e.to_upsert_tuple() for e in entities]
        with self.db.connection() as conn:
            with conn.cursor() as cur:
                cur.executemany(sql, tuples)
                conn.commit()
        logger.debug(f"Upsert {len(tuples)} 条 ETF 记录到数据库")
