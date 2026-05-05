"""
行业分类体系同步任务

将申万一二级、东财二级行业分类从 akshare 同步到数据库的 industry_category 表。
"""
import logging
from typing import List, Dict

import akshare as ak
import pandas as pd

from collector.db.postgres import PostgresDB
from collector.models import IndustryCategory

logger = logging.getLogger(__name__)


def sync_sw_industries(db: PostgresDB) -> None:
    """同步申万行业分类（一级 + 二级）"""
    logger.info("开始同步申万行业分类...")

    categories: List[IndustryCategory] = []
    name_to_code: Dict[str, str] = {}

    try:
        # 申万一级
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
        # 申万二级
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
        db.upsert_many(sql, params)
        logger.info(f"同步申万行业分类完成，共 {len(categories)} 条")


def sync_em_industries(db: PostgresDB) -> None:
    """同步东方财富行业板块（作为二级分类）"""
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
        db.upsert_many(sql, params)
        logger.info(f"同步东财行业板块完成，共 {len(categories)} 条")


def run(db: PostgresDB) -> None:
    """执行全量行业分类同步"""
    sync_sw_industries(db)
    sync_em_industries(db)
    logger.info("行业分类体系同步任务完成")


if __name__ == "__main__":
    import os
    from dotenv import load_dotenv

    load_dotenv()
    db = PostgresDB(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        database=os.getenv("DB_NAME", "security_analyze"),
        user=os.getenv("DB_USER", "stock"),
        password=os.getenv("DB_PASSWORD", "stock"),
    )
    run(db)
