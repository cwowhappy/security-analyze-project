"""任务执行器单元测试。"""



from data_collector.config import Settings
from data_collector.core.domain.collection_task import CollectionTask
from data_collector.core.domain.company import Company
from data_collector.core.domain.stock import Stock
from data_collector.core.ports.company_repository import CompanyRepository
from data_collector.core.ports.data_source import DataSource, SourceHealth, SourceStatus
from data_collector.core.ports.stock_repository import StockRepository
from data_collector.task_executor import TaskExecutor


class MockSource(DataSource):
    """测试用数据源。"""

    def __init__(self, name: str = "mock", priority: int = 1, available: bool = True) -> None:
        self._name = name
        self._priority = priority
        self._available = available
        self.fetch_stock_list_calls = 0
        self.fetch_company_info_calls = 0

    @property
    def name(self) -> str:
        return self._name

    @property
    def priority(self) -> int:
        return self._priority

    def fetch_stock_list(self) -> list[Stock]:
        self.fetch_stock_list_calls += 1
        return [
            Stock(stock_code="000001", name="平安银行"),
            Stock(stock_code="000002", name="万科A"),
        ]

    def fetch_company_info(self, stock_code: str) -> Company | None:
        self.fetch_company_info_calls += 1
        if stock_code == "NOTFOUND":
            return None
        return Company(name=f"公司_{stock_code}")

    def check_health(self) -> SourceHealth:
        status = SourceStatus.HEALTHY if self._available else SourceStatus.UNAVAILABLE
        return SourceHealth(status=status, latency_ms=10.0, error_rate=0.0, last_check="")


class MockStockRepo(StockRepository):
    """测试用股票仓库。"""

    def __init__(self) -> None:
        self.saved: list[Stock] = []

    def save(self, stock: Stock) -> None:
        self.saved.append(stock)

    def save_all(self, stocks: list[Stock]) -> tuple[int, int]:
        self.saved.extend(stocks)
        return len(stocks), 0

    def find_by_symbol(self, stock_code: str) -> Stock | None:
        for s in self.saved:
            if s.stock_code == stock_code:
                return s
        return None

    def find_all(self) -> list[Stock]:
        return self.saved

    def count(self) -> int:
        return len(self.saved)


class MockCompanyRepo(CompanyRepository):
    """测试用公司仓库。"""

    def __init__(self) -> None:
        self.saved: list[Company] = []

    def save(self, company: Company) -> None:
        self.saved.append(company)

    def save_all(self, companies: list[Company]) -> tuple[int, int]:
        self.saved.extend(companies)
        return len(companies), 0

    def find_by_usc_code(self, usc_code: str) -> Company | None:
        return None

    def find_all(self) -> list[Company]:
        return self.saved

    def count(self) -> int:
        return len(self.saved)


class TestTaskExecutor:
    """TaskExecutor 测试。"""

    def setup_method(self) -> None:
        self.source = MockSource()
        self.stock_repo = MockStockRepo()
        self.company_repo = MockCompanyRepo()
        self.executor = TaskExecutor(
            sources=[self.source],
            stock_repo=self.stock_repo,
            company_repo=self.company_repo,
            settings=Settings(),
        )

    def test_should_execute_stock_full(self) -> None:
        task = CollectionTask(task_type="stock_full")
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.task_type == "stock_full"
        assert result.total_count == 2
        assert result.success_count == 2
        assert self.source.fetch_stock_list_calls == 1

    def test_should_execute_company_full(self) -> None:
        # 先存入股票
        self.stock_repo.saved = [
            Stock(stock_code="000001", name="平安银行"),
            Stock(stock_code="000002", name="万科A"),
        ]
        task = CollectionTask(task_type="company_full")
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 2
        assert result.success_count == 2
        assert len(self.company_repo.saved) == 2

    def test_should_execute_stock_single(self) -> None:
        task = CollectionTask(
            task_type="stock_single",
            task_params={"stock_code": "000001"},
        )
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 1
        assert result.success_count == 1

    def test_should_fail_stock_single_when_not_found(self) -> None:
        # 让 fetch_stock_list 返回不匹配的数据
        self.source.fetch_stock_list = lambda: [Stock(stock_code="999999", name="其他")]
        task = CollectionTask(
            task_type="stock_single",
            task_params={"stock_code": "000001"},
        )
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert result.fail_count == 1

    def test_should_execute_company_single(self) -> None:
        task = CollectionTask(
            task_type="company_single",
            task_params={"stock_code": "000001"},
        )
        result = self.executor.execute(task)

        assert result.status == "success"
        assert result.total_count == 1
        assert result.success_count == 1
        assert len(self.company_repo.saved) == 1

    def test_should_fail_company_single_when_not_found(self) -> None:
        task = CollectionTask(
            task_type="company_single",
            task_params={"stock_code": "NOTFOUND"},
        )
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert result.fail_count == 1

    def test_should_fail_on_unknown_task_type(self) -> None:
        task = CollectionTask(task_type="unknown_type")
        result = self.executor.execute(task)

        assert result.status == "failed"
        assert "未知的任务类型" in (result.error_message or "")

    def test_should_use_preferred_data_source(self) -> None:
        source_a = MockSource(name="a", priority=1)
        source_b = MockSource(name="b", priority=2)
        executor = TaskExecutor(
            sources=[source_a, source_b],
            stock_repo=self.stock_repo,
            company_repo=self.company_repo,
        )
        task = CollectionTask(task_type="stock_full", data_source="b")
        result = executor.execute(task)

        assert result.status == "success"
        assert result.data_source == "b"
        assert source_b.fetch_stock_list_calls == 1

    def test_should_fallback_when_preferred_unavailable(self) -> None:
        source_a = MockSource(name="a", priority=1, available=False)
        source_b = MockSource(name="b", priority=2)
        executor = TaskExecutor(
            sources=[source_a, source_b],
            stock_repo=self.stock_repo,
            company_repo=self.company_repo,
        )
        task = CollectionTask(task_type="stock_full", data_source="a")
        result = executor.execute(task)

        assert result.status == "success"
        assert result.data_source == "b"

    def test_should_fail_when_all_sources_unavailable(self) -> None:
        source_a = MockSource(name="a", priority=1, available=False)
        executor = TaskExecutor(
            sources=[source_a],
            stock_repo=self.stock_repo,
            company_repo=self.company_repo,
        )
        task = CollectionTask(task_type="stock_full")
        result = executor.execute(task)
        assert result.status == "failed"

    def test_should_use_multi_source_when_akshare_and_tushare_available(self) -> None:
        """当 akshare 与 tushare 均可用时，stock_full 应使用双源合并模式。"""
        ak = MockSource(name="akshare", priority=1)
        ts = MockSource(name="tushare", priority=2)
        executor = TaskExecutor(
            sources=[ak, ts],
            stock_repo=self.stock_repo,
            company_repo=self.company_repo,
        )
        task = CollectionTask(task_type="stock_full")
        result = executor.execute(task)

        assert result.status == "success"
        assert result.data_source == "akshare+tushare"
        # 双源模式下两个数据源都应被调用
        assert ak.fetch_stock_list_calls == 1
        assert ts.fetch_stock_list_calls == 1

    def test_should_fallback_to_single_source_when_tushare_unavailable(self) -> None:
        """tushare 不可用时，stock_full 应回退到单源模式。"""
        ak = MockSource(name="akshare", priority=1)
        ts = MockSource(name="tushare", priority=2, available=False)
        executor = TaskExecutor(
            sources=[ak, ts],
            stock_repo=self.stock_repo,
            company_repo=self.company_repo,
        )
        task = CollectionTask(task_type="stock_full")
        result = executor.execute(task)

        assert result.status == "success"
        assert result.data_source == "akshare"
        assert ak.fetch_stock_list_calls == 1
        assert ts.fetch_stock_list_calls == 0
