"""股票领域模型。"""

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal


@dataclass(frozen=True)
class Stock:
    """股票领域实体。"""

    symbol: str
    name: str
    market: str
    current_price: Decimal | None = None
    change_percent: Decimal | None = None
    updated_at: datetime | None = None

    def __post_init__(self) -> None:
        if not self.symbol:
            raise ValueError("股票代码不能为空")
        if not self.name:
            raise ValueError("股票名称不能为空")
