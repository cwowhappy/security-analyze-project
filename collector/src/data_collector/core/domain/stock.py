"""股票领域模型，与 tb_stock_basic 表结构对应。"""

from dataclasses import dataclass
from datetime import date, datetime


@dataclass
class Stock:
    """股票基础信息领域实体。"""

    stock_code: str
    name: str
    id: str | None = None
    ts_code: str | None = None
    full_name: str | None = None
    market: str | None = None
    exchange: str | None = None
    list_date: date | None = None
    industry: str | None = None
    area: str | None = None
    total_shares: int | None = None
    float_shares: int | None = None
    company_id: str | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None

    def __post_init__(self) -> None:
        if not self.stock_code:
            raise ValueError("股票代码 stock_code 不能为空")
        if not self.name:
            raise ValueError("股票名称 name 不能为空")

    def to_dict(self) -> dict:
        """转换为字典（用于入库）。"""
        return {
            "id": self.id,
            "stock_code": self.stock_code,
            "ts_code": self.ts_code,
            "name": self.name,
            "full_name": self.full_name,
            "market": self.market,
            "exchange": self.exchange,
            "list_date": self.list_date,
            "industry": self.industry,
            "area": self.area,
            "total_shares": self.total_shares,
            "float_shares": self.float_shares,
            "company_id": self.company_id,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "Stock":
        """从字典创建实例（从数据库读取）。"""
        return cls(
            id=data.get("id"),
            stock_code=data["stock_code"],
            ts_code=data.get("ts_code"),
            name=data["name"],
            full_name=data.get("full_name"),
            market=data.get("market"),
            exchange=data.get("exchange"),
            list_date=data.get("list_date"),
            industry=data.get("industry"),
            area=data.get("area"),
            total_shares=data.get("total_shares"),
            float_shares=data.get("float_shares"),
            company_id=data.get("company_id"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )
