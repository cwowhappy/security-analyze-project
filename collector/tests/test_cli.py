"""CLI 子命令参数解析测试。

验证 argparse 子命令结构正确，不实际执行采集任务。
"""
import pytest
from collector.cli import build_parser


class TestCliParser:
    """测试 CLI 参数解析"""

    @pytest.fixture
    def parser(self):
        return build_parser()

    # ------------------------------------------------------------------
    # company
    # ------------------------------------------------------------------
    def test_company_full(self, parser):
        args = parser.parse_args(["company"])
        assert args.command == "company"
        assert args.stock_code is None

    def test_company_partial(self, parser):
        args = parser.parse_args(["company", "--stock-code", "600941"])
        assert args.command == "company"
        assert args.stock_code == "600941"

    def test_company_by_name(self, parser):
        args = parser.parse_args(["company", "--stock-code", "中国移动"])
        assert args.stock_code == "中国移动"

    # ------------------------------------------------------------------
    # finance
    # ------------------------------------------------------------------
    def test_finance_full(self, parser):
        args = parser.parse_args(["finance"])
        assert args.command == "finance"
        assert args.stock_code is None
        assert args.batch_size == 100
        assert args.incremental is False

    def test_finance_partial(self, parser):
        args = parser.parse_args(["finance", "--stock-code", "600941"])
        assert args.stock_code == "600941"

    def test_finance_with_options(self, parser):
        args = parser.parse_args([
            "finance",
            "--stock-code", "600519",
            "--start-year", "2020",
            "--end-year", "2024",
            "--incremental",
            "--batch-size", "50",
        ])
        assert args.stock_code == "600519"
        assert args.start_year == 2020
        assert args.end_year == 2024
        assert args.incremental is True
        assert args.batch_size == 50

    def test_finance_resume(self, parser):
        args = parser.parse_args(["finance", "--resume", "abc-123"])
        assert args.resume == "abc-123"

    # ------------------------------------------------------------------
    # industry
    # ------------------------------------------------------------------
    def test_industry_full(self, parser):
        args = parser.parse_args(["industry"])
        assert args.command == "industry"
        assert args.sw is False
        assert args.em is False

    def test_industry_sw_only(self, parser):
        args = parser.parse_args(["industry", "--sw"])
        assert args.sw is True
        assert args.em is False

    def test_industry_em_only(self, parser):
        args = parser.parse_args(["industry", "--em"])
        assert args.sw is False
        assert args.em is True

    def test_industry_both(self, parser):
        args = parser.parse_args(["industry", "--sw", "--em"])
        assert args.sw is True
        assert args.em is True

    # ------------------------------------------------------------------
    # index-basic
    # ------------------------------------------------------------------
    def test_index_basic(self, parser):
        args = parser.parse_args(["index-basic"])
        assert args.command == "index-basic"

    # ------------------------------------------------------------------
    # index-history
    # ------------------------------------------------------------------
    def test_index_history_full(self, parser):
        args = parser.parse_args(["index-history"])
        assert args.command == "index-history"
        assert args.index_code is None
        assert args.incremental is False
        assert args.resume is None

    def test_index_history_partial(self, parser):
        args = parser.parse_args(["index-history", "--index-code", "000001"])
        assert args.index_code == "000001"

    def test_index_history_incremental(self, parser):
        args = parser.parse_args(["index-history", "--incremental"])
        assert args.incremental is True

    def test_index_history_resume(self, parser):
        args = parser.parse_args(["index-history", "--resume", "xyz-789"])
        assert args.resume == "xyz-789"

    def test_index_history_partial_incremental(self, parser):
        args = parser.parse_args(["index-history", "--index-code", "000001", "--incremental"])
        assert args.index_code == "000001"
        assert args.incremental is True

    # ------------------------------------------------------------------
    # etf
    # ------------------------------------------------------------------
    def test_etf(self, parser):
        args = parser.parse_args(["etf"])
        assert args.command == "etf"

    # ------------------------------------------------------------------
    # quote
    # ------------------------------------------------------------------
    def test_quote_full(self, parser):
        args = parser.parse_args(["quote"])
        assert args.command == "quote"
        assert args.date is None
        assert args.stock_code is None

    def test_quote_with_date(self, parser):
        args = parser.parse_args(["quote", "--date", "2024-01-15"])
        assert args.date == "2024-01-15"

    def test_quote_with_stock_code(self, parser):
        args = parser.parse_args(["quote", "--stock-code", "600941"])
        assert args.stock_code == "600941"

    def test_quote_with_date_and_stock_code(self, parser):
        args = parser.parse_args(["quote", "--date", "2024-01-15", "--stock-code", "600941"])
        assert args.date == "2024-01-15"
        assert args.stock_code == "600941"

    # ------------------------------------------------------------------
    # schedule
    # ------------------------------------------------------------------
    def test_schedule_empty(self, parser):
        args = parser.parse_args(["schedule"])
        assert args.command == "schedule"
        assert args.company is None
        assert args.finance is None

    def test_schedule_single(self, parser):
        args = parser.parse_args(["schedule", "--company", "0 2 * * *"])
        assert args.company == "0 2 * * *"
        assert args.finance is None

    def test_schedule_multiple(self, parser):
        args = parser.parse_args([
            "schedule",
            "--company", "0 2 * * *",
            "--finance", "0 3 * * 1",
            "--index-history", "0 4 * * *",
        ])
        assert args.company == "0 2 * * *"
        assert args.finance == "0 3 * * 1"
        assert args.index_history == "0 4 * * *"
        assert args.quote is None

    # ------------------------------------------------------------------
    # 异常场景
    # ------------------------------------------------------------------
    def test_missing_subcommand_raises(self, parser):
        with pytest.raises(SystemExit):
            parser.parse_args([])

    def test_unknown_subcommand_raises(self, parser):
        with pytest.raises(SystemExit):
            parser.parse_args(["unknown"])
