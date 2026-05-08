"""行业分类体系同步任务（支持申万 SW 和东方财富 EM 分别同步）。"""
import logging
from typing import List, Dict

import akshare as ak
import pandas as pd

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor
from collector.models import IndustryCategory
from collector.tasks.base import BaseTask, TaskResult

logger = logging.getLogger(__name__)


class IndustryTask(BaseTask):
    """行业分类体系同步任务"""

    task_name = "industry_sync"
    data_type = "industry_category"

    def __init__(self, db: PostgresDB, source: BaseDataSource, monitor: Monitor = None):
        super().__init__(db=db, source=source, monitor=monitor)

    def run_full(self, **kwargs) -> TaskResult:
        """全量同步申万 + 东财行业分类。"""
        sw_rows = self._sync_sw()
        em_rows = self._sync_em()
        logger.info("行业分类体系同步任务完成")
        return TaskResult(rows=sw_rows + em_rows)

    def run_partial(self, identifiers: List[str], **kwargs) -> TaskResult:
        """指定同步来源：传入 ["SW"] 或 ["EM"] 或 ["SW", "EM"]。"""
        id_set = {s.upper() for s in identifiers}
        total_rows = 0

        if "SW" in id_set:
            sw_rows = self._sync_sw()
            total_rows += sw_rows
        if "EM" in id_set:
            em_rows = self._sync_em()
            total_rows += em_rows

        logger.info(f"行业分类指定同步完成，来源: {id_set}，共 {total_rows} 条")
        return TaskResult(rows=total_rows)

    def run_incremental(self, **kwargs) -> TaskResult:
        """增量采集（数据量小，暂按全量处理）。"""
        logger.info("行业分类增量采集暂按全量处理")
        return self.run_full(**kwargs)

    def _sync_sw(self) -> int:
        """同步申万行业分类（一级 + 二级），返回处理记录数。"""
        logger.info("开始同步申万行业分类...")
        categories: List[IndustryCategory] = []
        name_to_code: Dict[str, str] = {}

        try:
            sw1 = ak.sw_index_first_info()
            for _, row in sw1.iterrows():
                code = str(row.get("行业代码", "")).replace(".SI", "")
                name = str(row.get("行业名称", ""))
                name_to_code[name] = code
                categories.append(IndustryCategory(
                    standard_code="SW",
                    level=1,
                    code=code,
                    name=name,
                    sort_order=0,
                ))
            logger.info(f"获取申万一级行业 {len(sw1)} 个")
        except Exception as e:
            logger.warning(f"获取申万一级行业失败: {e}")

        try:
            sw2 = ak.sw_index_second_info()
            for _, row in sw2.iterrows():
                code = str(row.get("行业代码", "")).replace(".SI", "")
                name = str(row.get("行业名称", ""))
                parent_name = str(row.get("上级行业", "")) if pd.notna(row.get("上级行业")) else None
                parent_code = name_to_code.get(parent_name) if parent_name else None
                categories.append(IndustryCategory(
                    standard_code="SW",
                    level=2,
                    code=code,
                    name=name,
                    parent_code=parent_code,
                    sort_order=0,
                ))
            logger.info(f"获取申万二级行业 {len(sw2)} 个")
        except Exception as e:
            logger.warning(f"获取申万二级行业失败: {e}")

        if categories:
            sql = IndustryCategory.upsert_sql()
            params = [c.to_upsert_tuple() for c in categories]
            self.db.upsert_many(sql, params)
            logger.info(f"同步申万行业分类完成，共 {len(categories)} 条")
            return len(categories)
        return 0

    def _sync_em(self) -> int:
        """同步东方财富行业板块（作为二级分类），返回处理记录数。"""
        logger.info("开始同步东方财富行业板块...")
        categories: List[IndustryCategory] = []

        try:
            em = ak.stock_board_industry_name_em()
            for idx, row in em.iterrows():
                name = str(row.get("板块名称", ""))
                code = str(row.get("板块代码", ""))
                categories.append(IndustryCategory(
                    standard_code="EM",
                    level=2,
                    code=code,
                    name=name,
                    sort_order=idx,
                ))
            logger.info(f"获取东财行业板块 {len(em)} 个")
        except Exception as e:
            logger.warning(f"获取东财行业板块失败: {e}")

        if categories:
            sql = IndustryCategory.upsert_sql()
            params = [c.to_upsert_tuple() for c in categories]
            self.db.upsert_many(sql, params)
            logger.info(f"同步东财行业板块完成，共 {len(categories)} 条")
            return len(categories)
        return 0
