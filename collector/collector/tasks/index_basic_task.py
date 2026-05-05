import logging
from typing import List, Dict, Any

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor
from collector.models import IndexInfoEntity

logger = logging.getLogger(__name__)


class IndexBasicTask:
    """采集指数基本信息任务（全量 upsert）"""

    def __init__(self, db: PostgresDB, source: AkshareSource, monitor: Monitor = None):
        self.db = db
        self.source = source
        self.monitor = monitor

    def run(self) -> int:
        """执行采集，返回处理的记录数"""
        logger.info("开始采集指数基本信息")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start("index_basic", "index_basic")

        try:
            raw_list = self.source.get_index_list()
            logger.info(f"从数据源获取到 {len(raw_list)} 条指数记录")

            entities = [self._parse_index(item) for item in raw_list]
            entities = [e for e in entities if e is not None]

            if not entities:
                logger.warning("没有解析到有效的指数数据")
                if task_id:
                    self.monitor.log_task_end(task_id, "success", 0)
                return 0

            self._upsert_to_db(entities)

            if task_id:
                self.monitor.log_task_end(task_id, "success", len(entities))

            logger.info(f"指数基本信息采集完成，共 {len(entities)} 条")
            return len(entities)

        except Exception as e:
            logger.error(f"指数基本信息采集失败: {e}", exc_info=True)
            if task_id:
                self.monitor.log_task_end(task_id, "failed", error_message=str(e))
            raise

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
