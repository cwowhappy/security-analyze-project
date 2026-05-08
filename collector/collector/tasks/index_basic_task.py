import logging
from typing import List, Dict, Any

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor
from collector.models import IndexInfoEntity
from collector.tasks.base import BaseTask, TaskResult

logger = logging.getLogger(__name__)


class IndexBasicTask(BaseTask):
    """采集指数基本信息任务（全量 upsert）"""

    task_name = "index_basic"
    data_type = "index_basic"

    def __init__(self, db: PostgresDB, source: BaseDataSource, monitor: Monitor = None):
        super().__init__(db=db, source=source, monitor=monitor)

    def run(self, **kwargs) -> int:
        """向后兼容的手动执行入口，返回处理的记录数。"""
        result = self.run_full(**kwargs)
        return result.rows

    def run_full(self, **kwargs) -> TaskResult:
        """全量采集指数基本信息。"""
        logger.info("开始采集指数基本信息")
        raw_list = self.source.get_index_list()
        logger.info(f"从数据源获取到 {len(raw_list)} 条指数记录")

        entities = [self._parse_index(item) for item in raw_list]
        entities = [e for e in entities if e is not None]

        if not entities:
            logger.warning("没有解析到有效的指数数据")
            return TaskResult(rows=0)

        self._upsert_to_db(entities)
        logger.info(f"指数基本信息采集完成，共 {len(entities)} 条")
        return TaskResult(rows=len(entities))

    def run_partial(self, identifiers: List[str], **kwargs) -> TaskResult:
        """指定指数代码列表采集。"""
        logger.info(f"开始采集指定指数基本信息: {identifiers}")
        raw_list = self.source.get_index_list()
        id_set = set(identifiers)
        filtered = [item for item in raw_list if str(item.get("index_code", "")).strip() in id_set]
        logger.info(f"从数据源获取到 {len(filtered)} 条匹配指数记录")

        entities = [self._parse_index(item) for item in filtered]
        entities = [e for e in entities if e is not None]

        if not entities:
            logger.warning("没有解析到有效的指数数据")
            return TaskResult(rows=0)

        self._upsert_to_db(entities)
        logger.info(f"指定指数基本信息采集完成，共 {len(entities)} 条")
        return TaskResult(rows=len(entities))

    def run_incremental(self, **kwargs) -> TaskResult:
        """增量采集（数据量小，暂按全量处理）。"""
        logger.info("指数基本信息增量采集暂按全量处理")
        return self.run_full(**kwargs)

    def _parse_index(self, raw: Dict[str, Any]) -> IndexInfoEntity:
        """解析原始数据为 IndexInfoEntity"""
        try:
            index_code = str(raw.get("index_code", "")).strip()
            index_name = str(raw.get("display_name", "")).strip()
            if not index_code or not index_name:
                return None

            publish_date = str(raw.get("publish_date", "")).strip()
            if publish_date == "" or publish_date.lower() == "nan":
                publish_date = None

            return IndexInfoEntity(
                index_code=index_code,
                index_name=index_name,
                index_type=self._infer_index_type(index_code, index_name),
                market=self._infer_market(index_code),
                publish_date=publish_date,
            )
        except Exception as e:
            logger.debug(f"解析指数数据失败: {raw}, error={e}")
            return None

    @staticmethod
    def _infer_index_type(index_code: str, index_name: str) -> str:
        """根据指数代码和名称推断指数类型"""
        name = index_name.lower()
        if "上证" in index_name or "深证" in index_name or "沪深" in index_name or "中证" in index_name:
            if "行业" in index_name or "产业" in index_name:
                return "行业"
            if "主题" in index_name or "概念" in index_name:
                return "主题"
            return "宽基"
        if "行业" in index_name or "产业" in index_name:
            return "行业"
        if "主题" in index_name or "概念" in index_name:
            return "主题"
        if "策略" in index_name:
            return "策略"
        return "其他"

    @staticmethod
    def _infer_market(index_code: str) -> str:
        """根据指数代码推断市场"""
        if index_code.startswith("0") or index_code.startswith("9"):
            return "SH"
        if index_code.startswith("3") or index_code.startswith("8"):
            return "SZ"
        return "CN"

    def _upsert_to_db(self, entities: List[IndexInfoEntity]):
        """批量 upsert 到数据库"""
        sql = IndexInfoEntity.upsert_sql()
        tuples = [e.to_upsert_tuple() for e in entities]
        with self.db.connection() as conn:
            with conn.cursor() as cur:
                cur.executemany(sql, tuples)
                conn.commit()
        logger.debug(f"Upsert {len(tuples)} 条指数记录到数据库")
