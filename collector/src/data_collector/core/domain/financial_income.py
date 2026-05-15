"""利润表领域模型，与 tb_financial_income 表结构对应。"""

from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal


@dataclass
class FinancialIncome:
    """利润表领域实体。"""

    stock_code: str
    report_date: date
    id: str | None = None
    report_type: str = "Y"
    basic_eps: Decimal | None = None
    diluted_eps: Decimal | None = None
    total_revenue: Decimal | None = None
    revenue: Decimal | None = None
    operating_cost: Decimal | None = None
    gross_profit: Decimal | None = None
    selling_expense: Decimal | None = None
    admin_expense: Decimal | None = None
    rd_expense: Decimal | None = None
    financial_expense: Decimal | None = None
    operating_profit: Decimal | None = None
    total_profit: Decimal | None = None
    net_profit: Decimal | None = None
    np_parent_company: Decimal | None = None
    np_excl_nonrecurring: Decimal | None = None
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
            "basic_eps": self.basic_eps,
            "diluted_eps": self.diluted_eps,
            "total_revenue": self.total_revenue,
            "revenue": self.revenue,
            "operating_cost": self.operating_cost,
            "gross_profit": self.gross_profit,
            "selling_expense": self.selling_expense,
            "admin_expense": self.admin_expense,
            "rd_expense": self.rd_expense,
            "financial_expense": self.financial_expense,
            "operating_profit": self.operating_profit,
            "total_profit": self.total_profit,
            "net_profit": self.net_profit,
            "np_parent_company": self.np_parent_company,
            "np_excl_nonrecurring": self.np_excl_nonrecurring,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "FinancialIncome":
        """从字典创建实例（从数据库读取）。"""
        return cls(
            id=data.get("id"),
            stock_code=data["stock_code"],
            report_date=data["report_date"],
            report_type=data.get("report_type", "Y"),
            basic_eps=data.get("basic_eps"),
            diluted_eps=data.get("diluted_eps"),
            total_revenue=data.get("total_revenue"),
            revenue=data.get("revenue"),
            operating_cost=data.get("operating_cost"),
            gross_profit=data.get("gross_profit"),
            selling_expense=data.get("selling_expense"),
            admin_expense=data.get("admin_expense"),
            rd_expense=data.get("rd_expense"),
            financial_expense=data.get("financial_expense"),
            operating_profit=data.get("operating_profit"),
            total_profit=data.get("total_profit"),
            net_profit=data.get("net_profit"),
            np_parent_company=data.get("np_parent_company"),
            np_excl_nonrecurring=data.get("np_excl_nonrecurring"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )
