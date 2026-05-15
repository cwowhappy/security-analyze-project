"""财务指标领域模型，与 tb_financial_indicator 表结构对应。"""

from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal


@dataclass
class FinancialIndicator:
    """财务指标领域实体。"""

    stock_code: str
    report_date: date
    id: str | None = None
    report_type: str = "Y"

    # 盈利能力
    roe: Decimal | None = None
    roa: Decimal | None = None
    roic: Decimal | None = None
    gross_margin: Decimal | None = None
    net_margin: Decimal | None = None
    net_margin_excl: Decimal | None = None

    # 偿债能力
    debt_ratio: Decimal | None = None
    current_ratio: Decimal | None = None
    quick_ratio: Decimal | None = None
    net_debt_ratio: Decimal | None = None
    equity_ratio: Decimal | None = None

    # 运营效率
    dso: Decimal | None = None
    dio: Decimal | None = None
    dpo: Decimal | None = None
    ccc: Decimal | None = None
    asset_turnover: Decimal | None = None
    fixed_asset_turnover: Decimal | None = None

    # 成长性
    revenue_growth: Decimal | None = None
    np_parent_growth: Decimal | None = None
    np_excl_growth: Decimal | None = None
    cfo_growth: Decimal | None = None
    equity_growth: Decimal | None = None
    asset_growth: Decimal | None = None

    # 估值
    pe: Decimal | None = None
    pb: Decimal | None = None
    ps: Decimal | None = None
    peg: Decimal | None = None
    ev_ebitda: Decimal | None = None
    dividend_yield: Decimal | None = None
    market_cap: Decimal | None = None

    # 现金流质量
    cfo_to_np: Decimal | None = None

    # 元数据
    data_source: str | None = None
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
            "roe": self.roe,
            "roa": self.roa,
            "roic": self.roic,
            "gross_margin": self.gross_margin,
            "net_margin": self.net_margin,
            "net_margin_excl": self.net_margin_excl,
            "debt_ratio": self.debt_ratio,
            "current_ratio": self.current_ratio,
            "quick_ratio": self.quick_ratio,
            "net_debt_ratio": self.net_debt_ratio,
            "equity_ratio": self.equity_ratio,
            "dso": self.dso,
            "dio": self.dio,
            "dpo": self.dpo,
            "ccc": self.ccc,
            "asset_turnover": self.asset_turnover,
            "fixed_asset_turnover": self.fixed_asset_turnover,
            "revenue_growth": self.revenue_growth,
            "np_parent_growth": self.np_parent_growth,
            "np_excl_growth": self.np_excl_growth,
            "cfo_growth": self.cfo_growth,
            "equity_growth": self.equity_growth,
            "asset_growth": self.asset_growth,
            "pe": self.pe,
            "pb": self.pb,
            "ps": self.ps,
            "peg": self.peg,
            "ev_ebitda": self.ev_ebitda,
            "dividend_yield": self.dividend_yield,
            "market_cap": self.market_cap,
            "cfo_to_np": self.cfo_to_np,
            "data_source": self.data_source,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "FinancialIndicator":
        """从字典创建实例（从数据库读取）。"""
        return cls(
            id=data.get("id"),
            stock_code=data["stock_code"],
            report_date=data["report_date"],
            report_type=data.get("report_type", "Y"),
            roe=data.get("roe"),
            roa=data.get("roa"),
            roic=data.get("roic"),
            gross_margin=data.get("gross_margin"),
            net_margin=data.get("net_margin"),
            net_margin_excl=data.get("net_margin_excl"),
            debt_ratio=data.get("debt_ratio"),
            current_ratio=data.get("current_ratio"),
            quick_ratio=data.get("quick_ratio"),
            net_debt_ratio=data.get("net_debt_ratio"),
            equity_ratio=data.get("equity_ratio"),
            dso=data.get("dso"),
            dio=data.get("dio"),
            dpo=data.get("dpo"),
            ccc=data.get("ccc"),
            asset_turnover=data.get("asset_turnover"),
            fixed_asset_turnover=data.get("fixed_asset_turnover"),
            revenue_growth=data.get("revenue_growth"),
            np_parent_growth=data.get("np_parent_growth"),
            np_excl_growth=data.get("np_excl_growth"),
            cfo_growth=data.get("cfo_growth"),
            equity_growth=data.get("equity_growth"),
            asset_growth=data.get("asset_growth"),
            pe=data.get("pe"),
            pb=data.get("pb"),
            ps=data.get("ps"),
            peg=data.get("peg"),
            ev_ebitda=data.get("ev_ebitda"),
            dividend_yield=data.get("dividend_yield"),
            market_cap=data.get("market_cap"),
            cfo_to_np=data.get("cfo_to_np"),
            data_source=data.get("data_source"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )
