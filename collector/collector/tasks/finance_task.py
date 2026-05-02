import logging
from typing import List, Dict, Any, Optional
from datetime import datetime
import pandas as pd

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.monitor import Monitor

logger = logging.getLogger(__name__)


class FinanceTask:
    """采集财务报告数据任务"""

    def __init__(self, db: PostgresDB, source: AkshareSource, monitor: Monitor = None):
        self.db = db
        self.source = source
        self.monitor = monitor

    def run(self, start_year: Optional[int] = None, end_year: Optional[int] = None, incremental: bool = False):
        """全量采集所有 A 股公司的财务报告（从 company_security 表读取股票列表）"""
        logger.info("Starting full finance task...")
        task_id = None
        if self.monitor:
            task_id = self.monitor.log_task_start("sync_finance_report", "finance_report")

        try:
            stock_codes = self._get_stock_codes_from_db()
            total = len(stock_codes)
            created = 0
            updated = 0
            failed = 0

            for stock_code in stock_codes:
                if not stock_code:
                    continue
                try:
                    c, u = self._collect_by_stock_code(stock_code, start_year=start_year, end_year=end_year, incremental=incremental)
                    created += c
                    updated += u
                except Exception as e:
                    logger.error(f"Failed to collect finance for {stock_code}: {e}")
                    failed += 1

            rows = created + updated
            if self.monitor:
                status = "success" if failed == 0 else "failed"
                self.monitor.log_task_end(task_id, status, rows)
                self.monitor.upsert_data_status("finance_report", rows, task_id)

            logger.info(
                f"Finance task finished. Total stocks: {total}, Created: {created}, Updated: {updated}, Failed: {failed}"
            )
        except Exception as e:
            logger.error(f"Finance task failed: {e}")
            if self.monitor:
                self.monitor.log_task_end(task_id, "failed", 0, str(e))
            raise

    def run_by_stock_code(self, stock_code: str, incremental: bool = False):
        """按股票代码采集指定公司的全部财务报告"""
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
        except Exception as e:
            logger.error(f"Failed to collect finance for {stock_code}: {e}")
            if self.monitor:
                self.monitor.log_task_end(task_id, "failed", 0, str(e))
            raise

    def run_by_stock_code_and_years(self, stock_code: str, start_year: int, end_year: int, incremental: bool = False):
        """按股票代码和年份范围采集指定公司的财务报告"""
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
            logger.warning(f"Incomplete data for {stock_code}, skipping")
            return 0, 0

        # 按 report_date 对齐三张表的数据
        reports = self._merge_reports(stock_code, balance_df, profit_df, cashflow_df)

        if not reports:
            logger.info(f"No reports to process for {stock_code}")
            return 0, 0

        # 增量过滤：只保留最新报告期之后的数据
        if incremental:
            latest_date = self._get_latest_report_date(stock_code)
            if latest_date:
                original_count = len(reports)
                reports = [r for r in reports if r.get("report_date") and r["report_date"] > latest_date]
                filtered_count = original_count - len(reports)
                if filtered_count > 0:
                    logger.info(f"Incremental mode: filtered {filtered_count} existing reports for {stock_code}, remaining {len(reports)}")
                if not reports:
                    logger.info(f"Incremental mode: no new reports for {stock_code}, latest={latest_date}")
                    return 0, 0

        created = 0
        updated = 0
        max_report_date = None
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
                if rd and (max_report_date is None or rd > max_report_date):
                    max_report_date = rd
            except Exception as e:
                logger.error(f"Failed to upsert report {stock_code} {report.get('report_date')}: {e}")

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
                continue

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
        mask = df["REPORT_DATE"] == report_date
        rows = df[mask]
        if rows.empty:
            return None
        return rows.iloc[0]

    def _insert_report(self, report: Dict[str, Any]) -> str:
        """直接插入财务报告（跳过查重，用于增量模式），返回 insert"""
        import json

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
        existing = self.db.fetchone(
            "SELECT id FROM financial_report WHERE stock_code = %s AND report_date = %s",
            (report["stock_code"], report["report_date"]),
        )

        import json

        balance_sheet_json = json.dumps(report["balance_sheet"], ensure_ascii=False, default=str) if report.get("balance_sheet") else None
        profit_sheet_json = json.dumps(report["profit_sheet"], ensure_ascii=False, default=str) if report.get("profit_sheet") else None
        cash_flow_sheet_json = json.dumps(report["cash_flow_sheet"], ensure_ascii=False, default=str) if report.get("cash_flow_sheet") else None

        if existing:
            # UPDATE
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
