import json
import logging
import uuid
from typing import List, Dict, Any, Optional, Set
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

import pandas as pd
import numpy as np

from collector.db.postgres import PostgresDB
from collector.sources.base import BaseDataSource
from collector.sources.akshare_source import (
    AkshareSource, COL_REPORT_DATE, COL_REPORT_TYPE, COL_NOTICE_DATE, COL_CURRENCY
)
from collector.monitor import Monitor
from collector.utils import infer_market
from collector.models import FinancialReport
from collector.tasks.base import BaseTask, TaskResult
from collector.tasks.fundamental_metrics_task import FundamentalMetricsTask

logger = logging.getLogger(__name__)

# 核心指标到 DataFrame 列名的映射
_BALANCE_FIELDS = {
    "total_assets": "TOTAL_ASSETS",
    "total_liabilities": "TOTAL_LIABILITIES",
    "total_equity": "TOTAL_EQUITY",
    "monetary_funds": "MONETARYFUNDS",
    "accounts_receivable": "ACCOUNTS_RECE",
    "inventory": "INVENTORY",
    "total_current_assets": "TOTAL_CURRENT_ASSETS",
    "total_noncurrent_assets": "TOTAL_NONCURRENT_ASSETS",
    "total_current_liabilities": "TOTAL_CURRENT_LIAB",
    "total_noncurrent_liabilities": "TOTAL_NONCURRENT_LIAB",
}

_PROFIT_FIELDS = {
    "total_revenue": "TOTAL_OPERATE_INCOME",
    "operate_income": "OPERATE_INCOME",
    "operate_cost": "OPERATE_COST",
    "sale_expense": "SALE_EXPENSE",
    "manage_expense": "MANAGE_EXPENSE",
    "research_expense": "RESEARCH_EXPENSE",
    "finance_expense": "FINANCE_EXPENSE",
    "operate_profit": "OPERATE_PROFIT",
    "total_profit": "TOTAL_PROFIT",
    "net_profit": "NETPROFIT",
    "parent_net_profit": "PARENT_NETPROFIT",
}

_CASHFLOW_FIELDS = {
    "operating_cash_flow": "NETCASH_OPERATE",
    "investing_cash_flow": "NETCASH_INVEST",
    "financing_cash_flow": "NETCASH_FINANCE",
    "cce_add": "CCE_ADD",
    "end_cce": "END_CCE",
}


class FinanceTask(BaseTask):
    """采集财务报告数据任务（支持 Session 级故障恢复、批次内并发、部分缺失容错）"""

    task_name = "finance_report"
    data_type = "finance_report"

    def __init__(
        self,
        db: PostgresDB,
        source: BaseDataSource,
        monitor: Monitor = None,
        max_workers: int = 3,
        batch_concurrent_workers: int = 3,
        auto_trigger_fundamental_metrics: bool = True,
    ):
        super().__init__(db=db, source=source, monitor=monitor)
        self._max_workers = max_workers
        self._batch_concurrent_workers = batch_concurrent_workers
        self._auto_trigger_fm = auto_trigger_fundamental_metrics

    # ------------------------------------------------------------------
    # BaseTask 接口实现
    # ------------------------------------------------------------------
    def run_full(
        self,
        start_year: Optional[int] = None,
        end_year: Optional[int] = None,
        incremental: bool = False,
        batch_size: int = 100,
        session_id: Optional[str] = None,
        **kwargs,
    ) -> TaskResult:
        """全量采集所有 A 股公司的财务报告。"""
        if session_id is not None and self.monitor is None:
            raise ValueError("恢复 session 需要提供 monitor 实例")

        if session_id is None:
            session_id, stock_codes, task_id = self._create_session(
                start_year, end_year, incremental, batch_size
            )
        else:
            session_id, stock_codes, task_id = self._resume_session(session_id)

        pending_codes = [code for code in stock_codes if code not in self._get_success_codes(session_id)]
        if not pending_codes:
            logger.info(f"Session {session_id} 所有股票已处理完成，无需继续")
            return TaskResult(rows=0)

        created, updated, failed = self._process_batches(
            pending_codes, session_id, task_id, start_year, end_year, incremental, batch_size
        )

        rows = created + updated
        logger.info(
            f"Finance task finished. Session: {session_id}, "
            f"Total stocks: {len(pending_codes)}, Created: {created}, Updated: {updated}, Failed: {failed}"
        )
        result = TaskResult(created=created, updated=updated, failed=failed, rows=rows)
        self._trigger_fundamental_metrics("full", processed_codes=pending_codes)
        return result

    def run_partial(
        self,
        identifiers: List[str],
        start_year: Optional[int] = None,
        end_year: Optional[int] = None,
        incremental: bool = False,
        **kwargs,
    ) -> TaskResult:
        """按股票代码列表采集指定公司的财务报告。"""
        total_created = 0
        total_updated = 0
        total_failed = 0
        for stock_code in identifiers:
            try:
                c, u = self._collect_by_stock_code(
                    stock_code, start_year=start_year, end_year=end_year, incremental=incremental
                )
                total_created += c
                total_updated += u
            except Exception as e:
                logger.error(f"Failed to collect finance for {stock_code}: {e}")
                total_failed += 1
        rows = total_created + total_updated
        result = TaskResult(created=total_created, updated=total_updated, failed=total_failed, rows=rows)
        self._trigger_fundamental_metrics("partial", identifiers=identifiers)
        return result

    def run_incremental(
        self,
        batch_size: int = 100,
        **kwargs,
    ) -> TaskResult:
        """增量采集：仅采集最新报告期之后的新增数据。

        注意：run_full 内部已自动触发衍生指标计算，此处不再重复触发。
        """
        return self.run_full(incremental=True, batch_size=batch_size, **kwargs)

    def resume_session(
        self,
        session_id: str,
        batch_size: int = 100,
        **kwargs,
    ) -> TaskResult:
        """从 Session 断点恢复。"""
        logger.info(f"恢复财务报告采集 Session {session_id}")
        return self.run_full(session_id=session_id, batch_size=batch_size, **kwargs)

    # ------------------------------------------------------------------
    # 触发衍生指标计算（阶段B）
    # ------------------------------------------------------------------
    def _trigger_fundamental_metrics(
        self,
        mode: str,
        identifiers: Optional[List[str]] = None,
        processed_codes: Optional[List[str]] = None,
    ) -> None:
        """在财务报告采集完成后，自动触发基本面衍生指标预计算。

        触发策略：
        - partial 模式（指定股票列表）：对这些股票做全量指标计算
        - full / incremental 模式：统一走增量模式，只计算最新年报年度，
          避免对大量股票做全量历史重算
        """
        if not self._auto_trigger_fm:
            return
        try:
            fm_task = FundamentalMetricsTask(
                db=self.db,
                source=self.source,
                monitor=self.monitor,
            )
            if mode == "partial" and identifiers:
                fm_task.execute(mode="partial", identifiers=identifiers)
            else:
                # full / incremental / resume 等模式统一走 incremental，
                # 只计算最新完整年报年度，避免全量历史重算
                fm_task.execute(mode="incremental")
            logger.info("[FinanceTask] 衍生指标预计算触发完成")
        except Exception as e:
            logger.error(f"[FinanceTask] 衍生指标预计算触发失败: {e}")

    # ------------------------------------------------------------------
    # 向后兼容
    # ------------------------------------------------------------------
    def run(
        self,
        start_year: Optional[int] = None,
        end_year: Optional[int] = None,
        incremental: bool = False,
        batch_size: int = 100,
        session_id: Optional[str] = None,
    ):
        """【向后兼容】全量采集所有 A 股公司的财务报告。"""
        result = self.run_full(
            start_year=start_year, end_year=end_year,
            incremental=incremental, batch_size=batch_size, session_id=session_id
        )
        return result.rows

    def run_by_stock_code(self, stock_code: str, incremental: bool = False) -> tuple[int, int]:
        """【向后兼容】按股票代码采集指定公司的全部财务报告，返回 (created, updated)。"""
        logger.info(f"Starting finance task for {stock_code}")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start("sync_finance_report_by_code", "finance_report")

        try:
            created, updated = self._collect_by_stock_code(stock_code, incremental=incremental)
            rows = created + updated
            if self.monitor:
                self.monitor.log_task_end(task_id, "success", rows)
            logger.info(f"Finance task for {stock_code} finished. Created: {created}, Updated: {updated}")
            return created, updated
        except Exception as e:
            logger.error(f"Failed to collect finance for {stock_code}: {e}")
            if self.monitor:
                self.monitor.log_task_end(task_id, "failed", 0, str(e))
            raise

    def run_by_stock_code_and_years(
        self, stock_code: str, start_year: int, end_year: int, incremental: bool = False
    ) -> tuple[int, int]:
        """【向后兼容】按股票代码和年份范围采集指定公司的财务报告，返回 (created, updated)。"""
        logger.info(f"Starting finance task for {stock_code}, years: {start_year}-{end_year}")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start(
                "sync_finance_report_by_code_and_years", "finance_report"
            )

        try:
            created, updated = self._collect_by_stock_code(
                stock_code, start_year=start_year, end_year=end_year, incremental=incremental
            )
            rows = created + updated
            if self.monitor:
                self.monitor.log_task_end(task_id, "success", rows)
            logger.info(f"Finance task for {stock_code} finished. Created: {created}, Updated: {updated}")
            return created, updated
        except Exception as e:
            logger.error(f"Failed to collect finance for {stock_code}: {e}")
            if self.monitor:
                self.monitor.log_task_end(task_id, "failed", 0, str(e))
            raise

    # ------------------------------------------------------------------
    # Session 管理
    # ------------------------------------------------------------------
    def _create_session(
        self, start_year, end_year, incremental, batch_size
    ) -> tuple[str, List[str], Optional[int]]:
        session_id = str(uuid.uuid4())
        stock_codes = self._get_stock_codes_from_db()
        total = len(stock_codes)
        params = {
            "stock_codes": stock_codes,
            "start_year": start_year,
            "end_year": end_year,
            "incremental": incremental,
            "batch_size": batch_size,
        }
        logger.info(f"创建新 Session {session_id}，共 {total} 只股票，batch_size={batch_size}")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start(
                "sync_finance_report", "finance_report", session_id=session_id, params=params
            )
        return session_id, stock_codes, task_id

    def _resume_session(self, session_id: str) -> tuple[str, List[str], Optional[int]]:
        params = self.monitor.get_session_params(session_id)
        if params is None:
            raise ValueError(f"Session {session_id} 不存在或已被清理")
        stock_codes = params.get("stock_codes") or self._get_stock_codes_from_db()
        start_year = params.get("start_year")
        end_year = params.get("end_year")
        incremental = params.get("incremental", False)
        batch_size = params.get("batch_size", 100)

        success_codes = self.monitor.get_session_progress(session_id)
        total = len(stock_codes)
        pending_count = total - len(success_codes)
        logger.info(
            f"恢复 Session {session_id}，总股票数 {total}，已成功 {len(success_codes)} 家，"
            f"剩余 {pending_count} 家，batch_size={batch_size}"
        )

        task_id = self.monitor.get_task_id_by_session(session_id)
        if task_id:
            self.monitor.update_task_status(task_id, "running")
        else:
            task_id = self.monitor.log_task_start(
                "sync_finance_report", "finance_report", session_id=session_id, params=params
            )
        return session_id, stock_codes, task_id

    def _get_success_codes(self, session_id: str) -> Set[str]:
        if self.monitor:
            return self.monitor.get_session_progress(session_id)
        return set()

    # ------------------------------------------------------------------
    # 批次处理（支持批次内并发）
    # ------------------------------------------------------------------
    def _process_batches(
        self,
        pending_codes: List[str],
        session_id: str,
        task_id: Optional[int],
        start_year: Optional[int],
        end_year: Optional[int],
        incremental: bool,
        batch_size: int,
    ) -> tuple[int, int, int]:
        created = 0
        updated = 0
        failed = 0
        total_pending = len(pending_codes)

        total_batches = (total_pending + batch_size - 1) // batch_size
        for batch_idx in range(total_batches):
            start = batch_idx * batch_size
            end = min(start + batch_size, total_pending)
            batch_codes = pending_codes[start:end]
            batch_num = batch_idx + 1

            logger.info(
                f"Processing batch {batch_num}/{total_batches} ({start + 1}-{end} / {total_pending})"
            )
            batch_created, batch_updated, batch_failed = self._process_batch(
                batch_codes, session_id, start_year, end_year, incremental
            )

            created += batch_created
            updated += batch_updated
            failed += batch_failed

            logger.info(
                f"Batch {batch_num}/{total_batches} finished. "
                f"Created: {batch_created}, Updated: {batch_updated}, Failed: {batch_failed}. "
                f"Total progress: {end}/{total_pending}"
            )

        return created, updated, failed

    def _process_batch(
        self,
        batch_codes: List[str],
        session_id: str,
        start_year: Optional[int],
        end_year: Optional[int],
        incremental: bool,
    ) -> tuple[int, int, int]:
        """处理单批次，支持批次内多线程并发采集。"""
        if self._batch_concurrent_workers <= 1:
            # 串行模式（向后兼容）
            return self._process_batch_serial(batch_codes, session_id, start_year, end_year, incremental)

        batch_created = 0
        batch_updated = 0
        batch_failed = 0
        results: Dict[str, tuple[int, int, str, Optional[str]]] = {}

        with ThreadPoolExecutor(max_workers=self._batch_concurrent_workers) as executor:
            futures = {
                executor.submit(
                    self._collect_by_stock_code, code, start_year, end_year, incremental
                ): code
                for code in batch_codes
            }
            for future in as_completed(futures):
                stock_code = futures[future]
                try:
                    c, u = future.result()
                    results[stock_code] = (c, u, "success", None)
                except Exception as e:
                    logger.error(f"Failed to collect finance for {stock_code}: {e}")
                    results[stock_code] = (0, 0, "failed", str(e))

        for stock_code in batch_codes:
            c, u, status, error = results.get(stock_code, (0, 0, "failed", "unknown"))
            if status == "success":
                batch_created += c
                batch_updated += u
            else:
                batch_failed += 1

            if self.monitor:
                self.monitor.log_task_progress(
                    session_id=session_id,
                    stock_code=stock_code,
                    status=status,
                    rows_created=c,
                    rows_updated=u,
                    error_message=error,
                )

        return batch_created, batch_updated, batch_failed

    def _process_batch_serial(
        self,
        batch_codes: List[str],
        session_id: str,
        start_year: Optional[int],
        end_year: Optional[int],
        incremental: bool,
    ) -> tuple[int, int, int]:
        """串行处理单批次（向后兼容）。"""
        batch_created = 0
        batch_updated = 0
        batch_failed = 0

        for stock_code in batch_codes:
            if not stock_code:
                continue
            stock_created = 0
            stock_updated = 0
            stock_status = "success"
            stock_error = None
            try:
                stock_created, stock_updated = self._collect_by_stock_code(
                    stock_code, start_year=start_year, end_year=end_year, incremental=incremental
                )
                batch_created += stock_created
                batch_updated += stock_updated
            except Exception as e:
                logger.error(f"Failed to collect finance for {stock_code}: {e}")
                batch_failed += 1
                stock_status = "failed"
                stock_error = str(e)

            if self.monitor:
                self.monitor.log_task_progress(
                    session_id=session_id,
                    stock_code=stock_code,
                    status=stock_status,
                    rows_created=stock_created,
                    rows_updated=stock_updated,
                    error_message=stock_error,
                )

        return batch_created, batch_updated, batch_failed

    # ------------------------------------------------------------------
    # 单只股票采集
    # ------------------------------------------------------------------
    def _get_stock_codes_from_db(self) -> List[str]:
        """从 company_security 表获取已上市 A 股的股票代码列表"""
        sql = """
            SELECT stock_code FROM company_security
            WHERE listing_status = 'listed' AND security_type = 'A股'
            ORDER BY stock_code
        """
        rows = self.db.fetchall(sql)
        return [row[0] for row in rows if row and row[0]]

    def _get_latest_report_date(self, stock_code: str) -> Optional[str]:
        """从同步状态表获取该股票已采集的最新报告期"""
        row = self.db.fetchone(
            "SELECT latest_report_date FROM collector_stock_sync_status WHERE stock_code = %s",
            (stock_code,),
        )
        if row and row[0]:
            return str(row[0])
        return None

    def _update_sync_status(self, stock_code: str, latest_report_date: str, report_count: int):
        """更新股票同步状态表"""
        sql = """
            INSERT INTO collector_stock_sync_status
                (stock_code, latest_report_date, report_count, last_sync_at, created_at, updated_at)
            VALUES (%s, %s, %s, NOW(), NOW(), NOW())
            ON CONFLICT (stock_code) DO UPDATE SET
                latest_report_date = EXCLUDED.latest_report_date,
                report_count = EXCLUDED.report_count,
                last_sync_at = EXCLUDED.last_sync_at,
                updated_at = EXCLUDED.updated_at
        """
        self.db.execute(sql, (stock_code, latest_report_date, report_count))

    def _collect_by_stock_code(
        self,
        stock_code: str,
        start_year: Optional[int] = None,
        end_year: Optional[int] = None,
        incremental: bool = False,
    ) -> tuple[int, int]:
        """采集单个公司的财务报告，返回 (created, updated)"""
        market = infer_market(stock_code) or "SH"
        symbol = f"{market}{stock_code}"

        # 并发获取三张报表
        sheets = self._fetch_sheets_concurrent(symbol, start_year, end_year)
        balance_df = sheets.get("balance")
        profit_df = sheets.get("profit")
        cashflow_df = sheets.get("cashflow")

        # 资产负债表为基准，必须存在；利润表/现金流量表允许缺失
        if balance_df is None or balance_df.empty:
            logger.warning(f"[{stock_code}] 资产负债表为空或缺失，跳过采集")
            return 0, 0

        reports = self._merge_reports(stock_code, balance_df, profit_df, cashflow_df)

        if not reports:
            logger.info(f"[{stock_code}] 无财务报告可处理")
            return 0, 0

        report_dates = [r.report_date for r in reports if r.report_date]
        date_range = f"{min(report_dates)} ~ {max(report_dates)}" if report_dates else "N/A"
        logger.info(f"[{stock_code}] 从数据源获取 {len(reports)} 条财务报告，日期范围: {date_range}")

        # 增量过滤
        if incremental:
            latest_date = self._get_latest_report_date(stock_code)
            if latest_date:
                original_count = len(reports)
                reports = [r for r in reports if r.report_date and r.report_date > latest_date]
                filtered_count = original_count - len(reports)
                if filtered_count > 0:
                    logger.info(
                        f"[{stock_code}] 增量模式过滤 {filtered_count} 条已存在报告（latest={latest_date}），"
                        f"剩余待处理 {len(reports)} 条"
                    )
                if not reports:
                    logger.info(f"[{stock_code}] 增量模式无新报告，最新报告期={latest_date}")
                    return 0, 0

        # 批量写入
        created, updated = self._bulk_write_reports(reports, incremental=incremental)

        processed_dates = [r.report_date for r in reports if r.report_date]
        max_report_date = max(processed_dates) if processed_dates else None

        logger.info(
            f"[{stock_code}] 财务报告采集完成，报告期数量: {len(processed_dates)}，"
            f"新建: {created}, 更新: {updated}"
        )

        # 更新同步状态（合并为单次查询或基于内存计算）
        if max_report_date:
            actual_max = max_report_date
            actual_count = len(processed_dates)
            self._update_sync_status(stock_code, actual_max, actual_count)

        return created, updated

    def _fetch_sheets_concurrent(
        self, symbol: str, start_year: Optional[int], end_year: Optional[int]
    ) -> Dict[str, Optional[pd.DataFrame]]:
        """并发获取三张财务报表"""
        tasks = {
            "balance": lambda: self.source.get_balance_sheet(symbol, start_year, end_year),
            "profit": lambda: self.source.get_profit_sheet(symbol, start_year, end_year),
            "cashflow": lambda: self.source.get_cash_flow_sheet(symbol, start_year, end_year),
        }
        results: Dict[str, Optional[pd.DataFrame]] = {}
        with ThreadPoolExecutor(max_workers=self._max_workers) as executor:
            futures = {executor.submit(fn): name for name, fn in tasks.items()}
            for future in as_completed(futures):
                name = futures[future]
                try:
                    results[name] = future.result()
                except Exception as e:
                    logger.warning(f"Failed to fetch {name} sheet for {symbol}: {e}")
                    results[name] = None
        return results

    # ------------------------------------------------------------------
    # 报表合并
    # ------------------------------------------------------------------
    def _merge_reports(
        self,
        stock_code: str,
        balance_df: pd.DataFrame,
        profit_df: Optional[pd.DataFrame],
        cashflow_df: Optional[pd.DataFrame],
    ) -> List[FinancialReport]:
        """将三张报表按 report_date 合并为统一的报告列表"""
        required_cols = [COL_REPORT_DATE, COL_REPORT_TYPE, COL_NOTICE_DATE, COL_CURRENCY]
        missing = [c for c in required_cols if c not in balance_df.columns]
        if missing:
            logger.warning(f"[{stock_code}] 资产负债表缺少必需列 {missing}，无法合并")
            return []

        merged = balance_df[[COL_REPORT_DATE, COL_REPORT_TYPE, COL_NOTICE_DATE, COL_CURRENCY]].copy()
        merged["stock_code"] = stock_code

        profit_index = self._build_date_index(profit_df)
        cashflow_index = self._build_date_index(cashflow_df)

        reports: List[FinancialReport] = []
        for _, row in merged.iterrows():
            report_date = self._parse_date(row.get(COL_REPORT_DATE))
            if not report_date:
                continue

            report_type = self._normalize_report_type(row.get(COL_REPORT_TYPE, ""))
            report_year = int(report_date[:4]) if report_date else None

            balance_row = self._find_row_by_date(balance_df, row[COL_REPORT_DATE])
            profit_row = profit_index.get(report_date)
            cashflow_row = cashflow_index.get(report_date)

            if balance_row is None:
                logger.warning(f"[{stock_code}] 报告期 {report_date} 在资产负债表中找不到对应行，跳过")
                continue

            if profit_row is None:
                logger.debug(
                    f"[{stock_code}] 报告期 {report_date} 在利润表中找不到对应行，利润表相关字段将留空"
                )
            if cashflow_row is None:
                logger.debug(
                    f"[{stock_code}] 报告期 {report_date} 在现金流量表中找不到对应行，现金流量表相关字段将留空"
                )

            report = self._build_report(
                stock_code, report_date, report_type, report_year,
                row, balance_row, profit_row, cashflow_row
            )
            reports.append(report)

        return reports

    @staticmethod
    def _build_date_index(df: Optional[pd.DataFrame]) -> Dict[str, pd.Series]:
        """将 DataFrame 按 REPORT_DATE 的前 10 位字符串建立字典索引"""
        index: Dict[str, pd.Series] = {}
        if df is None or df.empty or COL_REPORT_DATE not in df.columns:
            return index
        for _, row in df.iterrows():
            key = str(row[COL_REPORT_DATE])[:10]
            if key:
                index[key] = row
        return index

    @staticmethod
    def _find_row_by_date(df: pd.DataFrame, report_date) -> Optional[pd.Series]:
        """在 DataFrame 中查找指定 REPORT_DATE 的行"""
        if df is None or df.empty:
            return None
        target = str(report_date)[:10] if report_date is not None else None
        if target is None:
            return None
        mask = df[COL_REPORT_DATE].astype(str).str[:10] == target
        rows = df[mask]
        if rows.empty:
            return None
        return rows.iloc[0]

    def _build_report(
        self,
        stock_code: str,
        report_date: str,
        report_type: str,
        report_year: Optional[int],
        base_row: pd.Series,
        balance_row: pd.Series,
        profit_row: Optional[pd.Series],
        cashflow_row: Optional[pd.Series],
    ) -> FinancialReport:
        """构造单条财务报告对象"""
        data: Dict[str, Any] = {
            "stock_code": stock_code,
            "report_date": report_date,
            "report_type": report_type,
            "report_year": report_year,
            "notice_date": self._parse_date(base_row.get(COL_NOTICE_DATE)),
            "currency": base_row.get(COL_CURRENCY, "CNY"),
        }

        for field, col in _BALANCE_FIELDS.items():
            data[field] = self._parse_decimal(balance_row.get(col))

        for field, col in _PROFIT_FIELDS.items():
            data[field] = self._parse_decimal(profit_row.get(col)) if profit_row is not None else None

        for field, col in _CASHFLOW_FIELDS.items():
            data[field] = self._parse_decimal(cashflow_row.get(col)) if cashflow_row is not None else None

        data["balance_sheet"] = self._clean_dict(balance_row.to_dict()) if balance_row is not None else None
        data["profit_sheet"] = self._clean_dict(profit_row.to_dict()) if profit_row is not None else None
        data["cash_flow_sheet"] = self._clean_dict(cashflow_row.to_dict()) if cashflow_row is not None else None

        return FinancialReport.model_validate(data)

    # ------------------------------------------------------------------
    # 批量写入
    # ------------------------------------------------------------------
    def _bulk_write_reports(
        self, reports: List[FinancialReport], incremental: bool = False
    ) -> tuple[int, int]:
        """批量写入财务报告，返回 (created, updated)

        增量模式：直接 INSERT（数据已过滤，保证不存在）。
        全量模式：INSERT ... ON CONFLICT DO UPDATE。
        """
        if not reports:
            return 0, 0

        params_seq = [r.to_insert_tuple() for r in reports]

        if incremental:
            try:
                self.db.insert_many(FinancialReport.insert_sql(), params_seq)
                return len(reports), 0
            except Exception as e:
                logger.error(f"批量 INSERT 失败: {e}")
                return self._fallback_insert_one_by_one(reports)
        else:
            try:
                self.db.upsert_many(FinancialReport.upsert_sql(), params_seq)
                return 0, len(reports)
            except Exception as e:
                logger.error(f"批量 UPSERT 失败: {e}")
                return self._fallback_upsert_one_by_one(reports)

    def _fallback_insert_one_by_one(self, reports: List[FinancialReport]) -> tuple[int, int]:
        """降级为逐条 INSERT"""
        created = 0
        for report in reports:
            try:
                self.db.execute(FinancialReport.insert_sql(), report.to_insert_tuple())
                created += 1
            except Exception as ex:
                logger.error(f"逐条 INSERT 失败 {report.stock_code}/{report.report_date}: {ex}")
        return created, 0

    def _fallback_upsert_one_by_one(self, reports: List[FinancialReport]) -> tuple[int, int]:
        """降级为逐条 UPSERT（先查重再决定 INSERT/UPDATE）"""
        created = 0
        updated = 0
        for report in reports:
            try:
                existing = self.db.fetchone(
                    "SELECT id FROM financial_report WHERE stock_code = %s AND report_date = %s",
                    (report.stock_code, report.report_date),
                )
                if existing:
                    self.db.execute(FinancialReport.upsert_sql(), report.to_insert_tuple())
                    updated += 1
                else:
                    self.db.execute(FinancialReport.insert_sql(), report.to_insert_tuple())
                    created += 1
            except Exception as ex:
                logger.error(f"逐条 UPSERT 失败 {report.stock_code}/{report.report_date}: {ex}")
        return created, updated

    # ------------------------------------------------------------------
    # 数据清洗与解析
    # ------------------------------------------------------------------
    @staticmethod
    def _clean_dict(data: Dict[str, Any]) -> Dict[str, Any]:
        """将字典中的 NaN / numpy 标量替换为 Python 原生类型，便于 JSON 序列化"""
        cleaned = {}
        for k, v in data.items():
            if v is None:
                cleaned[k] = None
            elif isinstance(v, float) and (pd.isna(v) or np.isnan(v)):
                cleaned[k] = None
            elif isinstance(v, (np.integer, np.floating)):
                cleaned[k] = v.item()
            elif isinstance(v, np.ndarray):
                cleaned[k] = v.tolist()
            else:
                cleaned[k] = v
        return cleaned

    @staticmethod
    def _normalize_report_type(report_type: str) -> str:
        """标准化报告类型"""
        if not report_type:
            return report_type
        rt = str(report_type)
        if "年报" in rt:
            return "年报"
        elif "中报" in rt or "半年报" in rt:
            return "中报"
        elif "一季报" in rt or "第1季" in rt or "第一季" in rt:
            return "一季报"
        elif "三季报" in rt or "第3季" in rt or "第三季" in rt:
            return "三季报"
        return rt

    @staticmethod
    def _parse_date(date_val) -> Optional[str]:
        """解析日期为 YYYY-MM-DD"""
        if date_val is None or pd.isna(date_val):
            return None
        try:
            if isinstance(date_val, str):
                date_val = date_val.strip()
                if " " in date_val:
                    date_val = date_val.split(" ")[0]
                dt = datetime.strptime(date_val, "%Y-%m-%d")
            else:
                dt = pd.to_datetime(date_val)
            return dt.strftime("%Y-%m-%d")
        except (ValueError, TypeError):
            return None

    @staticmethod
    def _parse_decimal(val) -> Optional[float]:
        """解析数值"""
        if val is None or pd.isna(val):
            return None
        try:
            return float(val)
        except (ValueError, TypeError):
            return None
