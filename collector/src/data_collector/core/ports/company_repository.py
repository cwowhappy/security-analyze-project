"""公司仓库接口（Port）。"""

from abc import ABC, abstractmethod
from collections.abc import Sequence

from data_collector.core.domain.company import Company


class CompanyRepository(ABC):
    """公司仓库抽象接口。"""

    @abstractmethod
    def save(self, company: Company) -> None:
        """保存或更新公司数据。"""

    @abstractmethod
    def save_all(self, companies: Sequence[Company]) -> tuple[int, int]:
        """批量保存公司，返回 (成功数, 失败数)。"""

    @abstractmethod
    def find_by_usc_code(self, usc_code: str) -> Company | None:
        """根据统一社会信用代码查询。"""

    @abstractmethod
    def find_all(self) -> Sequence[Company]:
        """查询所有公司。"""

    @abstractmethod
    def count(self) -> int:
        """返回公司总数。"""
