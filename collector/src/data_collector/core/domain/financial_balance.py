"""资产负债表领域模型，与 tb_financial_balance 表结构对应。"""

from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal


@dataclass
class FinancialBalance:
    """资产负债表领域实体。"""

    stock_code: str
    report_date: date
    id: str | None = None
    report_type: str = "Y"
    total_assets: Decimal | None = None
    total_liabilities: Decimal | None = None
    total_equity: Decimal | None = None
    equity_parent_company: Decimal | None = None
    current_assets: Decimal | None = None
    non_current_assets: Decimal | None = None
    cash_equivalents: Decimal | None = None
    accounts_receivable: Decimal | None = None
    inventories: Decimal | None = None
    current_liabilities: Decimal | None = None
    non_current_liabilities: Decimal | None = None
    accounts_payable: Decimal | None = None
    short_term_borrowings: Decimal | None = None
    long_term_borrowings: Decimal | None = None
    goodwill: Decimal | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None

    def __post_init__(self) -> None:
        if not self.stock_code:
            raise ValueError("股票代码 stock_code 不能为空")
        if not self.report_date:
            raise ValueError("报告期 report_date 不能为空")

    def to_dict(self) -> dict:
        """转换为字典（用于入库）。"""
        return {
            "id": self.id,
            "stock_code": self.stock_code,
            "report_date": self.report_date,
            "report_type": self.report_type,
            "total_assets": self.total_assets,
            "total_liabilities": self.total_liabilities,
            "total_equity": self.total_equity,
            "equity_parent_company": self.equity_parent_company,
            "current_assets": self.current_assets,
            "non_current_assets": self.non_current_assets,
            "cash_equivalents": self.cash_equivalents,
            "accounts_receivable": self.accounts_receivable,
            "inventories": self.inventories,
            "current_liabilities": self.current_liabilities,
            "non_current_liabilities": self.non_current_liabilities,
            "accounts_payable": self.accounts_payable,
            "short_term_borrowings": self.short_term_borrowings,
            "long_term_borrowings": self.long_term_borrowings,
            "goodwill": self.goodwill,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "FinancialBalance":
        """从字典创建实例（从数据库读取）。"""
        return cls(
            id=data.get("id"),
            stock_code=data["stock_code"],
            report_date=data["report_date"],
            report_type=data.get("report_type", "Y"),
            total_assets=data.get("total_assets"),
            total_liabilities=data.get("total_liabilities"),
            total_equity=data.get("total_equity"),
            equity_parent_company=data.get("equity_parent_company"),
            current_assets=data.get("current_assets"),
            non_current_assets=data.get("non_current_assets"),
            cash_equivalents=data.get("cash_equivalents"),
            accounts_receivable=data.get("accounts_receivable"),
            inventories=data.get("inventories"),
            current_liabilities=data.get("current_liabilities"),
            non_current_liabilities=data.get("non_current_liabilities"),
            accounts_payable=data.get("accounts_payable"),
            short_term_borrowings=data.get("short_term_borrowings"),
            long_term_borrowings=data.get("long_term_borrowings"),
            goodwill=data.get("goodwill"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )
