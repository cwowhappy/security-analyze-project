import logging
import uuid
from typing import List, Dict, Any, Optional, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed

import pandas as pd

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.monitor import Monitor
from collector.models import IndexHistoryEntity
from collector.tasks.base import BaseTask, TaskResult

logger = logging.getLogger(__name__)

GRANULARITY_MAP = {
    "day": "daily",
    "week": "weekly",
    "month": "monthly",
}


class IndexHistoryTask(BaseTask):
    """采集指数历史行情任务（支持天/周/月三种粒度，Session 级故障恢复）"""

    task_name = "index_history"
    data_type = "index_history"

    def __init__(
        self,
        db: PostgresDB,
        source: BaseDataSource,
        monitor: Monitor = None,
        max_workers: int = 3,
    ):
        super().__init__(db=db, source=source, monitor=monitor)
        self._max_workers = max_workers

    # ------------------------------------------------------------------
    # 向后兼容
    # ------------------------------------------------------------------
    def run(
        self,
        index_codes: Optional[List[str]] = None,
        granularities: Optional[List[str]] = None,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        session_id: Optional[str] = None,
        incremental: bool = False,
    ):
        """向后兼容的手动执行入口。"""
        if incremental:
            result = self.run_incremental(
                index_codes=index_codes,
                granularities=granularities,
                start_date=start_date,
                end_date=end_date,
            )
        elif session_id:
            result = self.resume_session(
                session_id=session_id,
                index_codes=index_codes,
                granularities=granularities,
                start_date=start_date,
                end_date=end_date,
            )
        else:
            result = self.run_full(
                index_codes=index_codes,
                granularities=granularities,
                start_date=start_date,
                end_date=end_date,
            )
        return result.rows

    # ------------------------------------------------------------------
    # BaseTask 接口实现
    # ------------------------------------------------------------------
    def run_full(
        self,
        index_codes: Optional[List[str]] = None,
        granularities: Optional[List[str]] = None,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        session_id: Optional[str] = None,
        **kwargs,
    ) -> TaskResult:
        """全量采集指数历史行情。"""
        if granularities is None:
            granularities = ["day", "week", "month"]
        if index_codes is None:
            index_codes = self._get_all_index_codes()

        sid = session_id or str(uuid.uuid4())
        total_tasks = len(index_codes) * len(granularities)
        logger.info(f"开始采集指数历史行情，session={sid}，共 {total_tasks} 个任务")

        completed = 0
        failed = 0
        total_rows = 0

        try:
            success_set = self._load_success_set(sid)

            tasks: List[Tuple[str, str]] = []
            for code in index_codes:
                for gran in granularities:
                    task_key = f"{code}#{gran}"
                    if task_key not in success_set:
                        tasks.append((code, gran))

            logger.info(f"待处理任务数: {len(tasks)} / {total_tasks}")

            with ThreadPoolExecutor(max_workers=self._max_workers) as executor:
                future_to_task = {}
                for code, gran in tasks:
                    future = executor.submit(
                        self._collect_single, code, gran, start_date, end_date, sid
                    )
                    future_to_task[future] = (code, gran)

                for future in as_completed(future_to_task):
                    code, gran = future_to_task[future]
                    try:
                        rows = future.result()
                        completed += 1
                        total_rows += rows
                        logger.info(f"[{completed}/{len(tasks)}] {code}/{gran} 完成，写入 {rows} 条")
                    except Exception as e:
                        failed += 1
                        logger.error(f"{code}/{gran} 采集失败: {e}")

            logger.info(
                f"指数历史行情采集完成，session={sid}，"
                f"成功 {completed} 个，失败 {failed} 个，总写入 {total_rows} 条"
            )
            return TaskResult(rows=total_rows, failed=failed)

        except Exception as e:
            logger.error(f"指数历史行情采集异常: {e}", exc_info=True)
            raise

    def run_partial(
        self,
        identifiers: List[str],
        granularities: Optional[List[str]] = None,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        **kwargs,
    ) -> TaskResult:
        """指定指数代码列表采集。identifiers 为指数代码列表。"""
        return self.run_full(
            index_codes=identifiers,
            granularities=granularities,
            start_date=start_date,
            end_date=end_date,
        )

    def run_incremental(
        self,
        index_codes: Optional[List[str]] = None,
        granularities: Optional[List[str]] = None,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        **kwargs,
    ) -> TaskResult:
        """增量采集：基于已有数据的最大日期自动推断起始日期。"""
        max_dates = self._get_max_dates()
        logger.info(f"增量采集模式，已查询到 {len(max_dates)} 个已有数据的组合")

        if granularities is None:
            granularities = ["day", "week", "month"]
        if index_codes is None:
            index_codes = self._get_all_index_codes()

        sid = str(uuid.uuid4())
        total_tasks = len(index_codes) * len(granularities)
        logger.info(f"开始增量采集指数历史行情，session={sid}，共 {total_tasks} 个任务")

        completed = 0
        failed = 0
        total_rows = 0

        try:
            success_set = self._load_success_set(sid)

            tasks: List[Tuple[str, str]] = []
            for code in index_codes:
                for gran in granularities:
                    task_key = f"{code}#{gran}"
                    if task_key not in success_set:
                        tasks.append((code, gran))

            with ThreadPoolExecutor(max_workers=self._max_workers) as executor:
                future_to_task = {}
                for code, gran in tasks:
                    task_start_date = start_date
                    if task_start_date is None:
                        max_date = max_dates.get((code, gran))
                        if max_date:
                            next_date = pd.Timestamp(max_date) + pd.Timedelta(days=1)
                            task_start_date = next_date.strftime("%Y%m%d")
                            logger.debug(f"{code}/{gran} 增量起始日期: {task_start_date}")
                    future = executor.submit(
                        self._collect_single, code, gran, task_start_date, end_date, sid
                    )
                    future_to_task[future] = (code, gran)

                for future in as_completed(future_to_task):
                    code, gran = future_to_task[future]
                    try:
                        rows = future.result()
                        completed += 1
                        total_rows += rows
                        logger.info(f"[{completed}/{len(tasks)}] {code}/{gran} 完成，写入 {rows} 条")
                    except Exception as e:
                        failed += 1
                        logger.error(f"{code}/{gran} 采集失败: {e}")

            logger.info(
                f"指数历史行情增量采集完成，session={sid}，"
                f"成功 {completed} 个，失败 {failed} 个，总写入 {total_rows} 条"
            )
            return TaskResult(rows=total_rows, failed=failed)

        except Exception as e:
            logger.error(f"指数历史行情采集异常: {e}", exc_info=True)
            raise

    def resume_session(
        self,
        session_id: str,
        index_codes: Optional[List[str]] = None,
        granularities: Optional[List[str]] = None,
        start_date: Optional[str] = None,
        end_date: Optional[str] = None,
        **kwargs,
    ) -> TaskResult:
        """从 Session 断点恢复。"""
        logger.info(f"恢复 Session {session_id}")
        return self.run_full(
            index_codes=index_codes,
            granularities=granularities,
            start_date=start_date,
            end_date=end_date,
            session_id=session_id,
        )

    # ------------------------------------------------------------------
    # 单任务采集
    # ------------------------------------------------------------------
    def _collect_single(
        self,
        index_code: str,
        granularity: str,
        start_date: Optional[str],
        end_date: Optional[str],
        session_id: str,
    ) -> int:
        """采集单个指数+粒度的历史数据，返回写入行数"""
        ak_period = GRANULARITY_MAP.get(granularity, "daily")
        df = self.source.get_index_history(
            index_code, period=ak_period, start_date=start_date, end_date=end_date
        )

        if df is None or df.empty:
            self._mark_success(session_id, index_code, granularity, 0)
            return 0

        entities = self._parse_history_df(index_code, granularity, df)
        if not entities:
            self._mark_success(session_id, index_code, granularity, 0)
            return 0

        self._upsert_to_db(entities)
        self._mark_success(session_id, index_code, granularity, len(entities))
        return len(entities)

    # ------------------------------------------------------------------
    # 数据解析
    # ------------------------------------------------------------------
    def _parse_history_df(self, index_code: str, granularity: str, df: pd.DataFrame) -> List[IndexHistoryEntity]:
        """将 DataFrame 解析为 IndexHistoryEntity 列表"""
        entities = []
        for _, row in df.iterrows():
            try:
                trade_date = str(row.get("日期", "")).strip()
                if not trade_date or trade_date.lower() == "nan":
                    continue

                entity = IndexHistoryEntity(
                    index_code=index_code,
                    trade_date=trade_date,
                    granularity=granularity,
                    open_price=self._to_float(row.get("开盘")),
                    high_price=self._to_float(row.get("最高")),
                    low_price=self._to_float(row.get("最低")),
                    close_price=self._to_float(row.get("收盘")),
                    volume=self._to_int(row.get("成交量")),
                    amount=self._to_float(row.get("成交额")),
                    amplitude=self._to_float(row.get("振幅")),
                    change_pct=self._to_float(row.get("涨跌幅")),
                    change_amount=self._to_float(row.get("涨跌额")),
                    turnover_rate=self._to_float(row.get("换手率")),
                )
                entities.append(entity)
            except Exception as e:
                logger.debug(f"解析行情数据行失败: {row.to_dict()}, error={e}")
                continue

        return entities

    @staticmethod
    def _to_float(val) -> Optional[float]:
        if val is None or pd.isna(val):
            return None
        try:
            return float(val)
        except (ValueError, TypeError):
            return None

    @staticmethod
    def _to_int(val) -> Optional[int]:
        if val is None or pd.isna(val):
            return None
        try:
            return int(float(val))
        except (ValueError, TypeError):
            return None

    # ------------------------------------------------------------------
    # 数据库操作
    # ------------------------------------------------------------------
    def _get_all_index_codes(self) -> List[str]:
        """从数据库获取所有指数代码"""
        sql = "SELECT index_code FROM index_info ORDER BY index_code"
        with self.db.connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql)
                rows = cur.fetchall()
                return [r[0] for r in rows]

    def _get_max_dates(self) -> Dict[Tuple[str, str], str]:
        """查询 index_history 中每个 (index_code, granularity) 的最大 trade_date"""
        sql = """
            SELECT index_code, granularity, MAX(trade_date)::text
            FROM index_history
            GROUP BY index_code, granularity
        """
        result = {}
        try:
            with self.db.connection() as conn:
                with conn.cursor() as cur:
                    cur.execute(sql)
                    for row in cur.fetchall():
                        result[(row[0], row[1])] = row[2]
        except Exception as e:
            logger.warning(f"查询已有数据最大日期失败: {e}")
        return result

    def _upsert_to_db(self, entities: List[IndexHistoryEntity]):
        """批量 upsert 到数据库"""
        if not entities:
            return
        sql = IndexHistoryEntity.upsert_sql()
        tuples = [e.to_upsert_tuple() for e in entities]
        with self.db.connection() as conn:
            with conn.cursor() as cur:
                cur.executemany(sql, tuples)
                conn.commit()

    def _load_success_set(self, session_id: str) -> set:
        """加载已成功的任务集合，返回 task_key 集合（格式：index_code#granularity）"""
        if self.monitor:
            return self.monitor.get_session_progress(session_id)
        return set()

    def _mark_success(self, session_id: str, index_code: str, granularity: str, rows: int):
        """标记任务成功，task_key 格式：index_code#granularity"""
        if self.monitor:
            task_key = f"{index_code}#{granularity}"
            self.monitor.log_task_progress(
                session_id=session_id,
                task_key=task_key,
                status="success",
                rows_updated=rows,
            )
