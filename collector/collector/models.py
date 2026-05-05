"""Pydantic 数据模型

为 company、security、financial_report 定义结构化模型，
替代裸字典传递，提供类型安全与 to_db_tuple() 便捷输出。
"""
import json
from typing import Optional, List, Any
from datetime import date
from pydantic import BaseModel, Field


class CompanyEntity(BaseModel):
    """公司法人实体"""

    company_name: str
    short_name: Optional[str] = None
    industry: Optional[str] = None
    region: Optional[str] = None
    establish_date: Optional[str] = None  # YYYY-MM-DD
    registered_capital: Optional[float] = None

    def to_insert_tuple(self) -> tuple:
        """输出按 company INSERT 字段顺序的参数元组（不含 id）"""
        return (
            self.company_name,
            self.short_name,
            self.industry,
            self.region,
            self.establish_date,
            self.registered_capital,
        )

    def to_update_tuple(self) -> tuple:
        """输出按 company UPDATE 字段顺序的参数元组（不含 id）"""
        return (
            self.short_name,
            self.industry,
            self.region,
            self.establish_date,
            self.registered_capital,
        )


class IndustryCategory(BaseModel):
    """行业分类维度（支持多标准、多级）"""

    standard_code: str
    level: int
    code: str
    name: str
    parent_code: Optional[str] = None
    sort_order: int = 0

    def to_upsert_tuple(self) -> tuple:
        return (
            self.standard_code,
            self.level,
            self.code,
            self.name,
            self.parent_code,
            self.sort_order,
        )

    @classmethod
    def upsert_sql(cls) -> str:
        return """
            INSERT INTO industry_category (standard_code, level, code, name, parent_code, sort_order, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, NOW())
            ON CONFLICT (standard_code, level, code) DO UPDATE SET
                name = EXCLUDED.name,
                parent_code = EXCLUDED.parent_code,
                sort_order = EXCLUDED.sort_order,
                updated_at = EXCLUDED.updated_at
        """


class CompanyIndustryMapping(BaseModel):
    """公司与行业分类映射"""

    company_id: int
    standard_code: str
    level1_code: str
    level2_code: Optional[str] = None
    is_primary: bool = True

    def to_upsert_tuple(self) -> tuple:
        return (
            self.company_id,
            self.standard_code,
            self.level1_code,
            self.level2_code,
            self.is_primary,
        )

    @classmethod
    def upsert_sql(cls) -> str:
        return """
            INSERT INTO company_industry_mapping (company_id, standard_code, level1_code, level2_code, is_primary, created_at)
            VALUES (%s, %s, %s, %s, %s, NOW())
            ON CONFLICT (company_id, standard_code, level2_code) DO UPDATE SET
                level1_code = EXCLUDED.level1_code,
                is_primary = EXCLUDED.is_primary
        """


class SecurityEntity(BaseModel):
    """上市证券"""

    stock_code: str
    stock_name: str
    market: Optional[str] = None
    security_type: str = "A股"
    listing_date: Optional[str] = None  # YYYY-MM-DD
    listing_status: str = "listed"

    def to_insert_tuple(self, company_id: int) -> tuple:
        """输出按 company_security INSERT 字段顺序的参数元组（不含 id）"""
        return (
            company_id,
            self.stock_code,
            self.stock_name,
            self.market,
            self.security_type,
            self.listing_date,
            self.listing_status,
        )

    def to_update_tuple(self, company_id: int) -> tuple:
        """输出按 company_security UPDATE 字段顺序的参数元组（不含 id，stock_code 放最后用于 WHERE）"""
        return (
            company_id,
            self.stock_name,
            self.market,
            self.security_type,
            self.listing_date,
            self.listing_status,
            self.stock_code,
        )


class IndexInfoEntity(BaseModel):
    """指数基本信息"""

    index_code: str
    index_name: str
    index_type: Optional[str] = None
    market: str = "CN"
    base_date: Optional[str] = None
    base_point: Optional[float] = None
    component_count: Optional[int] = None
    publish_date: Optional[str] = None
    source: str = "akshare"

    def to_upsert_tuple(self) -> tuple:
        return (
            self.index_code,
            self.index_name,
            self.index_type,
            self.market,
            self.base_date,
            self.base_point,
            self.component_count,
            self.publish_date,
            self.source,
        )

    @classmethod
    def upsert_sql(cls) -> str:
        return """
            INSERT INTO index_info (index_code, index_name, index_type, market, base_date, base_point, component_count, publish_date, source, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
            ON CONFLICT (index_code) DO UPDATE SET
                index_name = EXCLUDED.index_name,
                index_type = EXCLUDED.index_type,
                market = EXCLUDED.market,
                base_date = EXCLUDED.base_date,
                base_point = EXCLUDED.base_point,
                component_count = EXCLUDED.component_count,
                publish_date = EXCLUDED.publish_date,
                source = EXCLUDED.source,
                updated_at = NOW()
        """


class IndexHistoryEntity(BaseModel):
    """指数历史行情"""

    index_code: str
    trade_date: str
    granularity: str = "day"
    open_price: Optional[float] = None
    high_price: Optional[float] = None
    low_price: Optional[float] = None
    close_price: Optional[float] = None
    volume: Optional[int] = None
    amount: Optional[float] = None
    amplitude: Optional[float] = None
    change_pct: Optional[float] = None
    change_amount: Optional[float] = None
    turnover_rate: Optional[float] = None

    def to_upsert_tuple(self) -> tuple:
        return (
            self.index_code,
            self.trade_date,
            self.granularity,
            self.open_price,
            self.high_price,
            self.low_price,
            self.close_price,
            self.volume,
            self.amount,
            self.amplitude,
            self.change_pct,
            self.change_amount,
            self.turnover_rate,
        )

    @classmethod
    def upsert_sql(cls) -> str:
        return """
            INSERT INTO index_history (index_code, trade_date, granularity, open_price, high_price, low_price, close_price, volume, amount, amplitude, change_pct, change_amount, turnover_rate, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
            ON CONFLICT (index_code, trade_date, granularity) DO UPDATE SET
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                volume = EXCLUDED.volume,
                amount = EXCLUDED.amount,
                amplitude = EXCLUDED.amplitude,
                change_pct = EXCLUDED.change_pct,
                change_amount = EXCLUDED.change_amount,
                turnover_rate = EXCLUDED.turnover_rate,
                updated_at = NOW()
        """


class EtfInfoEntity(BaseModel):
    """ETF 基本信息"""

    etf_code: str
    etf_name: str
    tracking_index_code: Optional[str] = None
    management_fee: Optional[float] = None
    fund_size: Optional[float] = None
    establish_date: Optional[str] = None
    market: str = "CN"
    source: str = "akshare"

    def to_upsert_tuple(self) -> tuple:
        return (
            self.etf_code,
            self.etf_name,
            self.tracking_index_code,
            self.management_fee,
            self.fund_size,
            self.establish_date,
            self.market,
            self.source,
        )

    @classmethod
    def upsert_sql(cls) -> str:
        return """
            INSERT INTO etf_info (etf_code, etf_name, tracking_index_code, management_fee, fund_size, establish_date, market, source, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
            ON CONFLICT (etf_code) DO UPDATE SET
                etf_name = EXCLUDED.etf_name,
                tracking_index_code = EXCLUDED.tracking_index_code,
                management_fee = EXCLUDED.management_fee,
                fund_size = EXCLUDED.fund_size,
                establish_date = EXCLUDED.establish_date,
                market = EXCLUDED.market,
                source = EXCLUDED.source,
                updated_at = NOW()
        """


class FinancialReport(BaseModel):
    """财务报告"""

    stock_code: str
    report_date: str  # YYYY-MM-DD
    report_type: str
    report_year: Optional[int] = None
    notice_date: Optional[str] = None
    currency: str = "CNY"

    # 资产负债表
    total_assets: Optional[float] = None
    total_liabilities: Optional[float] = None
    total_equity: Optional[float] = None
    monetary_funds: Optional[float] = None
    accounts_receivable: Optional[float] = None
    inventory: Optional[float] = None
    total_current_assets: Optional[float] = None
    total_noncurrent_assets: Optional[float] = None
    total_current_liabilities: Optional[float] = None
    total_noncurrent_liabilities: Optional[float] = None

    # 利润表
    total_revenue: Optional[float] = None
    operate_income: Optional[float] = None
    operate_cost: Optional[float] = None
    sale_expense: Optional[float] = None
    manage_expense: Optional[float] = None
    research_expense: Optional[float] = None
    finance_expense: Optional[float] = None
    operate_profit: Optional[float] = None
    total_profit: Optional[float] = None
    net_profit: Optional[float] = None
    parent_net_profit: Optional[float] = None

    # 现金流量表
    operating_cash_flow: Optional[float] = None
    investing_cash_flow: Optional[float] = None
    financing_cash_flow: Optional[float] = None
    cce_add: Optional[float] = None
    end_cce: Optional[float] = None

    # 完整 JSONB
    balance_sheet: Optional[dict] = None
    profit_sheet: Optional[dict] = None
    cash_flow_sheet: Optional[dict] = None

    def to_insert_tuple(self) -> tuple:
        """输出按 financial_report INSERT 字段顺序的参数元组"""
        return (
            self.stock_code,
            self.report_date,
            self.report_type,
            self.report_year,
            self.notice_date,
            self.currency,
            self.total_assets,
            self.total_liabilities,
            self.total_equity,
            self.monetary_funds,
            self.accounts_receivable,
            self.inventory,
            self.total_current_assets,
            self.total_noncurrent_assets,
            self.total_current_liabilities,
            self.total_noncurrent_liabilities,
            self.total_revenue,
            self.operate_income,
            self.operate_cost,
            self.sale_expense,
            self.manage_expense,
            self.research_expense,
            self.finance_expense,
            self.operate_profit,
            self.total_profit,
            self.net_profit,
            self.parent_net_profit,
            self.operating_cash_flow,
            self.investing_cash_flow,
            self.financing_cash_flow,
            self.cce_add,
            self.end_cce,
            json.dumps(self.balance_sheet, ensure_ascii=False, default=str) if self.balance_sheet else None,
            json.dumps(self.profit_sheet, ensure_ascii=False, default=str) if self.profit_sheet else None,
            json.dumps(self.cash_flow_sheet, ensure_ascii=False, default=str) if self.cash_flow_sheet else None,
        )

    @classmethod
    def insert_sql(cls) -> str:
        return """
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

    @classmethod
    def upsert_sql(cls) -> str:
        return """
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
            ON CONFLICT (stock_code, report_date) DO UPDATE SET
                report_type = EXCLUDED.report_type,
                report_year = EXCLUDED.report_year,
                notice_date = EXCLUDED.notice_date,
                currency = EXCLUDED.currency,
                total_assets = EXCLUDED.total_assets,
                total_liabilities = EXCLUDED.total_liabilities,
                total_equity = EXCLUDED.total_equity,
                monetary_funds = EXCLUDED.monetary_funds,
                accounts_receivable = EXCLUDED.accounts_receivable,
                inventory = EXCLUDED.inventory,
                total_current_assets = EXCLUDED.total_current_assets,
                total_noncurrent_assets = EXCLUDED.total_noncurrent_assets,
                total_current_liabilities = EXCLUDED.total_current_liabilities,
                total_noncurrent_liabilities = EXCLUDED.total_noncurrent_liabilities,
                total_revenue = EXCLUDED.total_revenue,
                operate_income = EXCLUDED.operate_income,
                operate_cost = EXCLUDED.operate_cost,
                sale_expense = EXCLUDED.sale_expense,
                manage_expense = EXCLUDED.manage_expense,
                research_expense = EXCLUDED.research_expense,
                finance_expense = EXCLUDED.finance_expense,
                operate_profit = EXCLUDED.operate_profit,
                total_profit = EXCLUDED.total_profit,
                net_profit = EXCLUDED.net_profit,
                parent_net_profit = EXCLUDED.parent_net_profit,
                operating_cash_flow = EXCLUDED.operating_cash_flow,
                investing_cash_flow = EXCLUDED.investing_cash_flow,
                financing_cash_flow = EXCLUDED.financing_cash_flow,
                cce_add = EXCLUDED.cce_add,
                end_cce = EXCLUDED.end_cce,
                balance_sheet = EXCLUDED.balance_sheet,
                profit_sheet = EXCLUDED.profit_sheet,
                cash_flow_sheet = EXCLUDED.cash_flow_sheet,
                updated_at = NOW()
        """
