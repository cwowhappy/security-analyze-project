"""现金流量表领域模型，与 tb_financial_cashflow 表结构对应。"""

from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal


@dataclass
class FinancialCashflow:
    """现金流量表领域实体。"""

    stock_code: str
    report_date: date
    id: str | None = None
    report_type: str = "Y"
    cf_operating: Decimal | None = None
    cf_investing: Decimal | None = None
    cf_financing: Decimal | None = None
    net_cash_flow: Decimal | None = None
    free_cash_flow: Decimal | None = None
    capex: Decimal | None = None
    cash_received_operating: Decimal | None = None
    tax_paid: Decimal | None = None
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
            "cf_operating": self.cf_operating,
            "cf_investing": self.cf_investing,
            "cf_financing": self.cf_financing,
            "net_cash_flow": self.net_cash_flow,
            "free_cash_flow": self.free_cash_flow,
            "capex": self.capex,
            "cash_received_operating": self.cash_received_operating,
            "tax_paid": self.tax_paid,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "FinancialCashflow":
        """从字典创建实例（从数据库读取）。"""
        return cls(
            id=data.get("id"),
            stock_code=data["stock_code"],
            report_date=data["report_date"],
            report_type=data.get("report_type", "Y"),
            cf_operating=data.get("cf_operating"),
            cf_investing=data.get("cf_investing"),
            cf_financing=data.get("cf_financing"),
            net_cash_flow=data.get("net_cash_flow"),
            free_cash_flow=data.get("free_cash_flow"),
            capex=data.get("capex"),
            cash_received_operating=data.get("cash_received_operating"),
            tax_paid=data.get("tax_paid"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )
