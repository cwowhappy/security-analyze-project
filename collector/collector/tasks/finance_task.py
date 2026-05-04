import json
import logging
import uuid
from typing import List, Dict, Any, Optional, Set
from datetime import datetime
import pandas as pd

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor

logger = logging.getLogger(__name__)


class FinanceTask:
    """采集财务报告数据任务（支持 Session 级故障恢复）"""

    def __init__(self, db: PostgresDB, source: AkshareSource, monitor: Monitor = None):
        self.db = db
        self.source = source
        self.monitor = monitor

    def run(
        self,
        start_year: Optional[int] = None,
        end_year: Optional[int] = None,
        incremental: bool = False,
        batch_size: int = 100,
        session_id: Optional[str] = None,
    ):
        """全量采集所有 A 股公司的财务报告（从 company_security 表读取股票列表）

        Args:
            start_year: 起始年份
            end_year: 结束年份
            incremental: 是否增量采集
            batch_size: 每批次处理的公司数量，默认100
            session_id: 恢复用的 Session ID。为 None 时创建新 Session；不为 None 时从断点恢复
        """
        # 恢复模式必须提供 monitor
        if session_id is not None and self.monitor is None:
            raise ValueError("恢复 session 需要提供 monitor 实例")

        task_id = None
        stock_codes: List[str] = []
        success_codes: Set[str] = set()

        if session_id is None:
            # 新 Session：生成 UUID，保存参数快照
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
            if self.monitor:
                task_id = self.monitor.log_task_start("sync_finance_report", "finance_report", session_id=session_id, params=params)
        else:
            # 恢复 Session：读取参数和已完成的进度
            params = self.monitor.get_session_params(session_id)
            if params is None:
                raise ValueError(f"Session {session_id} 不存在或已被清理")
            stock_codes = params.get("stock_codes")
            if not stock_codes:
                stock_codes = self._get_stock_codes_from_db()
            # 恢复原始参数（用户无需在恢复时重新传入）
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
                # 若找不到 task_log（理论上不应发生），新建一条
                task_id = self.monitor.log_task_start("sync_finance_report", "finance_report", session_id=session_id, params=params)

        # 过滤已成功的股票
        pending_codes = [code for code in stock_codes if code not in success_codes]
        if not pending_codes:
            logger.info(f"Session {session_id} 所有股票已处理完成，无需继续")
            if self.monitor and task_id:
                self.monitor.log_task_end(task_id, "success", 0)
            return

        created = 0
        updated = 0
        failed = 0
        total_pending = len(pending_codes)

        # 按批次处理
        total_batches = (total_pending + batch_size - 1) // batch_size
        for batch_idx in range(total_batches):
            start = batch_idx * batch_size
            end = min(start + batch_size, total_pending)
            batch_codes = pending_codes[start:end]
            batch_num = batch_idx + 1

            logger.info(f"Processing batch {batch_num}/{total_batches} ({start + 1}-{end} / {total_pending})")
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

                # 逐只记录进度
                if self.monitor:
                    self.monitor.log_task_progress(
                        session_id=session_id,
                        stock_code=stock_code,
                        status=stock_status,
                        rows_created=stock_created,
                        rows_updated=stock_updated,
                        error_message=stock_error,
                    )

            created += batch_created
            updated += batch_updated
            failed += batch_failed

            logger.info(
                f"Batch {batch_num}/{total_batches} finished. "
                f"Created: {batch_created}, Updated: {batch_updated}, Failed: {batch_failed}. "
                f"Total progress: {end}/{total_pending}"
            )

        rows = created + updated
        if self.monitor:
            status = "success" if failed == 0 else "failed"
            self.monitor.log_task_end(task_id, status, rows)
            self.monitor.upsert_data_status("finance_report", rows, task_id)

        logger.info(
            f"Finance task finished. Session: {session_id}, "
            f"Total stocks: {total_pending}, Created: {created}, Updated: {updated}, Failed: {failed}"
        )

    def run_by_stock_code(self, stock_code: str, incremental: bool = False) -> tuple[int, int]:
        """按股票代码采集指定公司的全部财务报告，返回 (created, updated)"""
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

    def run_by_stock_code_and_years(self, stock_code: str, start_year: int, end_year: int, incremental: bool = False) -> tuple[int, int]:
        """按股票代码和年份范围采集指定公司的财务报告，返回 (created, updated)"""
        logger.info(f"Starting finance task for {stock_code}, years: {start_year}-{end_year}")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start("sync_finance_report_by_code_and_years", "finance_report")

        try:
            created, updated = self._collect_by_stock_code(stock_code, start_year=start_year, end_year=end_year, incremental=incremental)
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
        market = self.source.infer_market(stock_code)
        symbol = f"{market}{stock_code}"

        # 采集三张报表（支持年份范围过滤）
        balance_df = self.source.get_balance_sheet(symbol, start_year=start_year, end_year=end_year)
        profit_df = self.source.get_profit_sheet(symbol, start_year=start_year, end_year=end_year)
        cashflow_df = self.source.get_cash_flow_sheet(symbol, start_year=start_year, end_year=end_year)

        if balance_df is None or profit_df is None or cashflow_df is None:
            logger.warning(f"[{stock_code}] 数据不完整（三张表任一返回 None），跳过采集")
            return 0, 0

        if balance_df.empty or profit_df.empty or cashflow_df.empty:
            logger.warning(
                f"[{stock_code}] 某张报表过滤后为空 "
                f"（balance={len(balance_df)}, profit={len(profit_df)}, cashflow={len(cashflow_df)}），跳过采集"
            )
            return 0, 0

        # 按 report_date 对齐三张表的数据
        reports = self._merge_reports(stock_code, balance_df, profit_df, cashflow_df)

        if not reports:
            logger.info(f"[{stock_code}] 无财务报告可处理")
            return 0, 0

        report_dates = [r["report_date"] for r in reports if r.get("report_date")]
        logger.info(f"[{stock_code}] 从数据源获取 {len(reports)} 条财务报告，报告期: {report_dates}")

        # 增量过滤：只保留最新报告期之后的数据
        if incremental:
            latest_date = self._get_latest_report_date(stock_code)
            if latest_date:
                original_count = len(reports)
                reports = [r for r in reports if r.get("report_date") and r["report_date"] > latest_date]
                filtered_count = original_count - len(reports)
                if filtered_count > 0:
                    remaining_dates = [r["report_date"] for r in reports if r.get("report_date")]
                    logger.info(
                        f"[{stock_code}] 增量模式过滤 {filtered_count} 条已存在报告（latest={latest_date}），"
                        f"剩余待处理 {len(reports)} 条，报告期: {remaining_dates}"
                    )
                if not reports:
                    logger.info(f"[{stock_code}] 增量模式无新报告，最新报告期={latest_date}")
                    return 0, 0

        created = 0
        updated = 0
        max_report_date = None
        processed_dates = []
        for report in reports:
            try:
                if incremental:
                    # 增量模式下已知该报告不存在，直接 INSERT 跳过查重
                    result = self._insert_report(report)
                else:
                    result = self._upsert_report(report)
                if result == "insert":
                    created += 1
                elif result == "update":
                    updated += 1

                # 记录最大报告日期用于更新同步状态
                rd = report.get("report_date")
                if rd:
                    processed_dates.append(rd)
                    if max_report_date is None or rd > max_report_date:
                        max_report_date = rd
            except Exception as e:
                logger.error(f"[{stock_code}] 写入财务报告失败，报告期 {report.get('report_date')}: {e}")

        logger.info(
            f"[{stock_code}] 财务报告采集完成，报告期: {processed_dates}，"
            f"新建: {created}, 更新: {updated}"
        )

        # 更新同步状态表（以当前数据库中该股票的实际最大报告日期为准）
        if max_report_date:
            # 重新查询以确保状态表反映真实最新日期（考虑并发或其他写入）
            db_max = self.db.fetchone(
                "SELECT MAX(report_date) FROM financial_report WHERE stock_code = %s",
                (stock_code,),
            )
            actual_max = str(db_max[0]) if db_max and db_max[0] else max_report_date
            total_count = self.db.fetchone(
                "SELECT COUNT(*) FROM financial_report WHERE stock_code = %s",
                (stock_code,),
            )
            actual_count = total_count[0] if total_count else (created + updated)
            self._update_sync_status(stock_code, actual_max, actual_count)

        return created, updated

    def _merge_reports(
        self,
        stock_code: str,
        balance_df: pd.DataFrame,
        profit_df: pd.DataFrame,
        cashflow_df: pd.DataFrame,
    ) -> List[Dict[str, Any]]:
        """将三张报表按 report_date 合并为统一的报告列表"""
        # 检查必需列
        required_cols = ["REPORT_DATE", "REPORT_TYPE", "NOTICE_DATE", "CURRENCY"]
        missing = [c for c in required_cols if c not in balance_df.columns]
        if missing:
            logger.warning(f"[{stock_code}] 资产负债表缺少必需列 {missing}，无法合并")
            return []

        # 以资产负债表为基准，按 REPORT_DATE 关联
        merged = balance_df[["REPORT_DATE", "REPORT_TYPE", "NOTICE_DATE", "CURRENCY"]].copy()
        merged["stock_code"] = stock_code

        # 解析报告年份和类型
        reports = []
        for _, row in merged.iterrows():
            report_date = self._parse_date(row.get("REPORT_DATE"))
            if not report_date:
                continue

            report_type = self._normalize_report_type(row.get("REPORT_TYPE", ""))
            report_year = int(report_date[:4]) if report_date else None

            # 从三张表中分别提取该报告期的数据
            balance_row = self._find_row_by_date(balance_df, row["REPORT_DATE"])
            profit_row = self._find_row_by_date(profit_df, row["REPORT_DATE"])
            cashflow_row = self._find_row_by_date(cashflow_df, row["REPORT_DATE"])

            if balance_row is None:
                logger.warning(f"[{stock_code}] 报告期 {report_date} 在资产负债表中找不到对应行，跳过")
                continue

            if profit_row is None:
                logger.warning(f"[{stock_code}] 报告期 {report_date} 在利润表中找不到对应行，利润表相关字段将留空")

            if cashflow_row is None:
                logger.warning(f"[{stock_code}] 报告期 {report_date} 在现金流量表中找不到对应行，现金流量表相关字段将留空")

            report = {
                "stock_code": stock_code,
                "report_date": report_date,
                "report_type": report_type,
                "report_year": report_year,
                "notice_date": self._parse_date(row.get("NOTICE_DATE")),
                "currency": row.get("CURRENCY", "CNY"),
                # 资产负债表核心指标
                "total_assets": self._parse_decimal(balance_row.get("TOTAL_ASSETS")),
                "total_liabilities": self._parse_decimal(balance_row.get("TOTAL_LIABILITIES")),
                "total_equity": self._parse_decimal(balance_row.get("TOTAL_EQUITY")),
                "monetary_funds": self._parse_decimal(balance_row.get("MONETARYFUNDS")),
                "accounts_receivable": self._parse_decimal(balance_row.get("ACCOUNTS_RECE")),
                "inventory": self._parse_decimal(balance_row.get("INVENTORY")),
                "total_current_assets": self._parse_decimal(balance_row.get("TOTAL_CURRENT_ASSETS")),
                "total_noncurrent_assets": self._parse_decimal(balance_row.get("TOTAL_NONCURRENT_ASSETS")),
                "total_current_liabilities": self._parse_decimal(balance_row.get("TOTAL_CURRENT_LIAB")),
                "total_noncurrent_liabilities": self._parse_decimal(balance_row.get("TOTAL_NONCURRENT_LIAB")),
                # 利润表核心指标
                "total_revenue": self._parse_decimal(profit_row.get("TOTAL_OPERATE_INCOME")) if profit_row is not None else None,
                "operate_income": self._parse_decimal(profit_row.get("OPERATE_INCOME")) if profit_row is not None else None,
                "operate_cost": self._parse_decimal(profit_row.get("OPERATE_COST")) if profit_row is not None else None,
                "sale_expense": self._parse_decimal(profit_row.get("SALE_EXPENSE")) if profit_row is not None else None,
                "manage_expense": self._parse_decimal(profit_row.get("MANAGE_EXPENSE")) if profit_row is not None else None,
                "research_expense": self._parse_decimal(profit_row.get("RESEARCH_EXPENSE")) if profit_row is not None else None,
                "finance_expense": self._parse_decimal(profit_row.get("FINANCE_EXPENSE")) if profit_row is not None else None,
                "operate_profit": self._parse_decimal(profit_row.get("OPERATE_PROFIT")) if profit_row is not None else None,
                "total_profit": self._parse_decimal(profit_row.get("TOTAL_PROFIT")) if profit_row is not None else None,
                "net_profit": self._parse_decimal(profit_row.get("NETPROFIT")) if profit_row is not None else None,
                "parent_net_profit": self._parse_decimal(profit_row.get("PARENT_NETPROFIT")) if profit_row is not None else None,
                # 现金流量表核心指标
                "operating_cash_flow": self._parse_decimal(cashflow_row.get("NETCASH_OPERATE")) if cashflow_row is not None else None,
                "investing_cash_flow": self._parse_decimal(cashflow_row.get("NETCASH_INVEST")) if cashflow_row is not None else None,
                "financing_cash_flow": self._parse_decimal(cashflow_row.get("NETCASH_FINANCE")) if cashflow_row is not None else None,
                "cce_add": self._parse_decimal(cashflow_row.get("CCE_ADD")) if cashflow_row is not None else None,
                "end_cce": self._parse_decimal(cashflow_row.get("END_CCE")) if cashflow_row is not None else None,
                # 完整 JSONB 数据（将 NaN 替换为 None）
                "balance_sheet": self._clean_dict(balance_row.to_dict()) if balance_row is not None else None,
                "profit_sheet": self._clean_dict(profit_row.to_dict()) if profit_row is not None else None,
                "cash_flow_sheet": self._clean_dict(cashflow_row.to_dict()) if cashflow_row is not None else None,
            }
            reports.append(report)

        return reports

    def _find_row_by_date(self, df: pd.DataFrame, report_date) -> Optional[pd.Series]:
        """在 DataFrame 中查找指定 REPORT_DATE 的行"""
        if df is None or df.empty:
            return None
        # 统一转换为标准化日期字符串比较，避免 Timestamp / str / datetime64 类型不一致导致匹配失败
        target = str(report_date)[:10] if report_date is not None else None
        if target is None:
            return None
        mask = df["REPORT_DATE"].astype(str).str[:10] == target
        rows = df[mask]
        if rows.empty:
            return None
        return rows.iloc[0]

    def _insert_report(self, report: Dict[str, Any]) -> str:
        """直接插入财务报告（跳过查重，用于增量模式），返回 insert"""
        stock_code = report.get("stock_code")
        report_date = report.get("report_date")
        logger.debug(f"[{stock_code}] INSERT 财务报告，报告期: {report_date}")

        balance_sheet_json = json.dumps(report["balance_sheet"], ensure_ascii=False, default=str) if report.get("balance_sheet") else None
        profit_sheet_json = json.dumps(report["profit_sheet"], ensure_ascii=False, default=str) if report.get("profit_sheet") else None
        cash_flow_sheet_json = json.dumps(report["cash_flow_sheet"], ensure_ascii=False, default=str) if report.get("cash_flow_sheet") else None

        sql = """
            INSERT INTO financial_report (
                stock_code, report_date, report_type, report_year, notice_date, currency,
                total_assets, total_liabilities, total_equity, monetary_funds, accounts_receivable,
                inventory, total_current_assets, total_noncurrent_assets, total_current_liabilities,
                total_noncurrent_liabilities, total_revenue, operate_income, operate_cost,
                sale_expense, manage_expense, research_expense, finance_expense, operate_profit,
                total_profit, net_profit, parent_net_profit, operating_cash_flow, investing_cash_flow,
                financing_cash_flow, cce_add, end_cce, balance_sheet, profit_sheet, cash_flow_sheet,
                created_at, updated_at
            ) VALUES (
                %s, %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s,
                %s, %s, %s, %s,
                %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s,
                %s::jsonb, %s::jsonb, %s::jsonb,
                NOW(), NOW()
            )
        """
        self.db.execute(sql, (
            report["stock_code"], report["report_date"], report["report_type"], report["report_year"],
            report["notice_date"], report["currency"],
            report["total_assets"], report["total_liabilities"], report["total_equity"],
            report["monetary_funds"], report["accounts_receivable"],
            report["inventory"], report["total_current_assets"], report["total_noncurrent_assets"],
            report["total_current_liabilities"], report["total_noncurrent_liabilities"],
            report["total_revenue"], report["operate_income"], report["operate_cost"],
            report["sale_expense"], report["manage_expense"], report["research_expense"],
            report["finance_expense"], report["operate_profit"], report["total_profit"],
            report["net_profit"], report["parent_net_profit"],
            report["operating_cash_flow"], report["investing_cash_flow"], report["financing_cash_flow"],
            report["cce_add"], report["end_cce"],
            balance_sheet_json, profit_sheet_json, cash_flow_sheet_json,
        ))
        return "insert"

    def _upsert_report(self, report: Dict[str, Any]) -> str:
        """插入或更新财务报告，返回 insert / update / skip"""
        stock_code = report.get("stock_code")
        report_date = report.get("report_date")

        existing = self.db.fetchone(
            "SELECT id FROM financial_report WHERE stock_code = %s AND report_date = %s",
            (stock_code, report_date),
        )

        balance_sheet_json = json.dumps(report["balance_sheet"], ensure_ascii=False, default=str) if report.get("balance_sheet") else None
        profit_sheet_json = json.dumps(report["profit_sheet"], ensure_ascii=False, default=str) if report.get("profit_sheet") else None
        cash_flow_sheet_json = json.dumps(report["cash_flow_sheet"], ensure_ascii=False, default=str) if report.get("cash_flow_sheet") else None

        if existing:
            # UPDATE
            logger.debug(f"[{stock_code}] UPDATE 财务报告，报告期: {report_date}")
            sql = """
                UPDATE financial_report SET
                    report_type = %s, report_year = %s, notice_date = %s, currency = %s,
                    total_assets = %s, total_liabilities = %s, total_equity = %s,
                    monetary_funds = %s, accounts_receivable = %s, inventory = %s,
                    total_current_assets = %s, total_noncurrent_assets = %s,
                    total_current_liabilities = %s, total_noncurrent_liabilities = %s,
                    total_revenue = %s, operate_income = %s, operate_cost = %s,
                    sale_expense = %s, manage_expense = %s, research_expense = %s,
                    finance_expense = %s, operate_profit = %s, total_profit = %s,
                    net_profit = %s, parent_net_profit = %s,
                    operating_cash_flow = %s, investing_cash_flow = %s, financing_cash_flow = %s,
                    cce_add = %s, end_cce = %s,
                    balance_sheet = %s::jsonb, profit_sheet = %s::jsonb, cash_flow_sheet = %s::jsonb,
                    updated_at = NOW()
                WHERE id = %s
            """
            self.db.execute(sql, (
                report["report_type"], report["report_year"], report["notice_date"], report["currency"],
                report["total_assets"], report["total_liabilities"], report["total_equity"],
                report["monetary_funds"], report["accounts_receivable"], report["inventory"],
                report["total_current_assets"], report["total_noncurrent_assets"],
                report["total_current_liabilities"], report["total_noncurrent_liabilities"],
                report["total_revenue"], report["operate_income"], report["operate_cost"],
                report["sale_expense"], report["manage_expense"], report["research_expense"],
                report["finance_expense"], report["operate_profit"], report["total_profit"],
                report["net_profit"], report["parent_net_profit"],
                report["operating_cash_flow"], report["investing_cash_flow"], report["financing_cash_flow"],
                report["cce_add"], report["end_cce"],
                balance_sheet_json, profit_sheet_json, cash_flow_sheet_json,
                existing[0],
            ))
            return "update"
        else:
            # INSERT
            logger.debug(f"[{stock_code}] INSERT 财务报告，报告期: {report_date}")
            sql = """
                INSERT INTO financial_report (
                    stock_code, report_date, report_type, report_year, notice_date, currency,
                    total_assets, total_liabilities, total_equity, monetary_funds, accounts_receivable,
                    inventory, total_current_assets, total_noncurrent_assets, total_current_liabilities,
                    total_noncurrent_liabilities, total_revenue, operate_income, operate_cost,
                    sale_expense, manage_expense, research_expense, finance_expense, operate_profit,
                    total_profit, net_profit, parent_net_profit, operating_cash_flow, investing_cash_flow,
                    financing_cash_flow, cce_add, end_cce, balance_sheet, profit_sheet, cash_flow_sheet,
                    created_at, updated_at
                ) VALUES (
                    %s, %s, %s, %s, %s, %s,
                    %s, %s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s::jsonb, %s::jsonb, %s::jsonb,
                    NOW(), NOW()
                )
            """
            self.db.execute(sql, (
                report["stock_code"], report["report_date"], report["report_type"], report["report_year"],
                report["notice_date"], report["currency"],
                report["total_assets"], report["total_liabilities"], report["total_equity"],
                report["monetary_funds"], report["accounts_receivable"],
                report["inventory"], report["total_current_assets"], report["total_noncurrent_assets"],
                report["total_current_liabilities"], report["total_noncurrent_liabilities"],
                report["total_revenue"], report["operate_income"], report["operate_cost"],
                report["sale_expense"], report["manage_expense"], report["research_expense"],
                report["finance_expense"], report["operate_profit"], report["total_profit"],
                report["net_profit"], report["parent_net_profit"],
                report["operating_cash_flow"], report["investing_cash_flow"], report["financing_cash_flow"],
                report["cce_add"], report["end_cce"],
                balance_sheet_json, profit_sheet_json, cash_flow_sheet_json,
            ))
            return "insert"

    @staticmethod
    def _clean_dict(data: Dict[str, Any]) -> Dict[str, Any]:
        """将字典中的 NaN 值替换为 None，便于 JSON 序列化"""
        cleaned = {}
        for k, v in data.items():
            if v is None:
                cleaned[k] = None
            elif isinstance(v, float) and pd.isna(v):
                cleaned[k] = None
            else:
                cleaned[k] = v
        return cleaned

    @staticmethod
    def _normalize_report_type(report_type: str) -> str:
        """标准化报告类型"""
        if "年报" in report_type:
            return "年报"
        elif "中报" in report_type or "半年报" in report_type:
            return "中报"
        elif "一季报" in report_type or "第1季" in report_type:
            return "一季报"
        elif "三季报" in report_type or "第3季" in report_type:
            return "三季报"
        return report_type

    @staticmethod
    def _parse_date(date_val) -> Optional[str]:
        """解析日期为 YYYY-MM-DD"""
        if date_val is None or pd.isna(date_val):
            return None
        try:
            if isinstance(date_val, str):
                # 处理 '2026-03-31 00:00:00' 和 '2026-03-31' 两种格式
                date_val = date_val.strip()
                if ' ' in date_val:
                    date_val = date_val.split(' ')[0]
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
