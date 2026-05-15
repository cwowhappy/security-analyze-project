"""财务指标计算适配器单元测试。"""

from datetime import date
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from data_collector.adapters.financial_indicator_calculated_adapter import (
    FinancialIndicatorCalculatedAdapter,
)
from data_collector.core.config.field_mapping_config import SourceConfig
from data_collector.core.domain.financial_balance import FinancialBalance
from data_collector.core.domain.financial_cashflow import FinancialCashflow
from data_collector.core.domain.financial_income import FinancialIncome
from data_collector.core.domain.financial_indicator import FinancialIndicator
from data_collector.core.domain.stock import Stock
from data_collector.services.indicator_calculator import FinancialDataSnapshot


class TestFinancialIndicatorCalculatedAdapter:
    """FinancialIndicatorCalculatedAdapter 测试。"""

    def _make_income(self, report_date: date, revenue: Decimal = Decimal("100")) -> FinancialIncome:
        return FinancialIncome(
            stock_code="000001",
            report_date=report_date,
            report_type="Y",
            revenue=revenue,
            net_profit=Decimal("10"),
            np_parent_company=Decimal("8"),
        )

    def _make_balance(self, report_date: date, total_assets: Decimal = Decimal("200")) -> FinancialBalance:
        return FinancialBalance(
            stock_code="000001",
            report_date=report_date,
            report_type="Y",
            total_assets=total_assets,
            total_liabilities=Decimal("100"),
            total_equity=Decimal("100"),
            equity_parent_company=Decimal("80"),
        )

    def _make_cashflow(self, report_date: date, cf_operating: Decimal = Decimal("15")) -> FinancialCashflow:
        return FinancialCashflow(
            stock_code="000001",
            report_date=report_date,
            report_type="Y",
            cf_operating=cf_operating,
        )

    def test_fetch_calculates_and_saves_indicator(self) -> None:
        income_repo = MagicMock()
        balance_repo = MagicMock()
        cashflow_repo = MagicMock()
        indicator_repo = MagicMock()
        stock_repo = MagicMock()
        calculator = MagicMock()

        d1 = date(2023, 12, 31)
        d2 = date(2022, 12, 31)

        income_repo.find_by_stock_code.return_value = [
            self._make_income(d1),
            self._make_income(d2),
        ]
        balance_repo.find_by_stock_code.return_value = [
            self._make_balance(d1),
            self._make_balance(d2),
        ]
        cashflow_repo.find_by_stock_code.return_value = [
            self._make_cashflow(d1),
            self._make_cashflow(d2),
        ]
        stock_repo.find_by_symbol.return_value = Stock(
            stock_code="000001", name="平安银行", industry="银行"
        )

        mock_indicator1 = FinancialIndicator(
            stock_code="000001",
            report_date=d1,
            report_type="Y",
            roe=Decimal("10.0"),
            data_source="CALCULATED",
        )
        mock_indicator2 = FinancialIndicator(
            stock_code="000001",
            report_date=d2,
            report_type="Y",
            roe=Decimal("9.0"),
            data_source="CALCULATED",
        )
        calculator.calculate.side_effect = [mock_indicator1, mock_indicator2]

        adapter = FinancialIndicatorCalculatedAdapter(
            income_repo=income_repo,
            balance_repo=balance_repo,
            cashflow_repo=cashflow_repo,
            indicator_repo=indicator_repo,
            stock_repo=stock_repo,
            calculator=calculator,
        )
        source = SourceConfig(
            name="calculated",
            adapter="financial_indicator_calculated_adapter",
            priority=1,
        )
        result = adapter.fetch("000001", source)

        assert len(result) == 2
        assert result[0]["stock_code"] == "000001"
        assert result[0]["roe"] == Decimal("10.0")
        assert result[1]["stock_code"] == "000001"
        assert result[1]["roe"] == Decimal("9.0")
        assert indicator_repo.save.call_count == 2
        assert calculator.calculate.call_count == 2
        _, kwargs = calculator.calculate.call_args
        assert kwargs["is_bank"] is True

    def test_fetch_returns_empty_when_income_missing(self) -> None:
        income_repo = MagicMock()
        income_repo.find_by_stock_code.return_value = []
        balance_repo = MagicMock()
        cashflow_repo = MagicMock()
        indicator_repo = MagicMock()

        adapter = FinancialIndicatorCalculatedAdapter(
            income_repo=income_repo,
            balance_repo=balance_repo,
            cashflow_repo=cashflow_repo,
            indicator_repo=indicator_repo,
        )
        source = SourceConfig(
            name="calculated",
            adapter="financial_indicator_calculated_adapter",
            priority=1,
        )
        result = adapter.fetch("000001", source)

        assert result == []
        indicator_repo.save.assert_not_called()

    def test_fetch_skips_when_balance_or_cashflow_mismatch(self) -> None:
        income_repo = MagicMock()
        balance_repo = MagicMock()
        cashflow_repo = MagicMock()
        indicator_repo = MagicMock()
        stock_repo = MagicMock()
        calculator = MagicMock()

        d1 = date(2023, 12, 31)

        income_repo.find_by_stock_code.return_value = [self._make_income(d1)]
        balance_repo.find_by_stock_code.return_value = [self._make_balance(d1)]
        cashflow_repo.find_by_stock_code.return_value = []  # 缺失现金流量表

        adapter = FinancialIndicatorCalculatedAdapter(
            income_repo=income_repo,
            balance_repo=balance_repo,
            cashflow_repo=cashflow_repo,
            indicator_repo=indicator_repo,
            stock_repo=stock_repo,
            calculator=calculator,
        )
        source = SourceConfig(
            name="calculated",
            adapter="financial_indicator_calculated_adapter",
            priority=1,
        )
        result = adapter.fetch("000001", source)

        assert result == []
        calculator.calculate.assert_not_called()

    def test_fetch_handles_calculator_exception(self) -> None:
        income_repo = MagicMock()
        balance_repo = MagicMock()
        cashflow_repo = MagicMock()
        indicator_repo = MagicMock()
        stock_repo = MagicMock()
        calculator = MagicMock()

        d1 = date(2023, 12, 31)

        income_repo.find_by_stock_code.return_value = [self._make_income(d1)]
        balance_repo.find_by_stock_code.return_value = [self._make_balance(d1)]
        cashflow_repo.find_by_stock_code.return_value = [self._make_cashflow(d1)]
        stock_repo.find_by_symbol.return_value = None
        calculator.calculate.side_effect = ZeroDivisionError("division by zero")

        adapter = FinancialIndicatorCalculatedAdapter(
            income_repo=income_repo,
            balance_repo=balance_repo,
            cashflow_repo=cashflow_repo,
            indicator_repo=indicator_repo,
            stock_repo=stock_repo,
            calculator=calculator,
        )
        source = SourceConfig(
            name="calculated",
            adapter="financial_indicator_calculated_adapter",
            priority=1,
        )
        result = adapter.fetch("000001", source)

        assert result == []
        indicator_repo.save.assert_not_called()
