"""Tests for EtfBasicTask."""
from unittest.mock import MagicMock
import pytest

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.etf_basic_task import EtfBasicTask
from collector.models import EtfInfoEntity


class TestEtfBasicTask:
    def _make_task(self):
        db = MagicMock(spec=PostgresDB)
        source = MagicMock(spec=AkshareSource)
        task = EtfBasicTask(db=db, source=source)
        return task, db, source

    def test_parse_etf_success(self):
        task, db, source = self._make_task()

        raw = {
            "代码": "510050",
            "名称": "华夏上证50ETF",
            "总市值": 1200000000.0,
            "流通市值": 1100000000.0,
        }

        entity = task._parse_etf(raw)

        assert entity is not None
        assert entity.etf_code == "510050"
        assert entity.etf_name == "华夏上证50ETF"
        assert entity.fund_size == 1200000000.0
        assert entity.market == "SH"
        assert entity.source == "akshare"

    def test_parse_etf_missing_code(self):
        task, db, source = self._make_task()
        raw = {"名称": "华夏上证50ETF"}
        assert task._parse_etf(raw) is None

    def test_parse_etf_missing_name(self):
        task, db, source = self._make_task()
        raw = {"代码": "510050"}
        assert task._parse_etf(raw) is None

    def test_parse_etf_fallback_fund_size(self):
        task, db, source = self._make_task()

        raw = {
            "代码": "510050",
            "名称": "华夏上证50ETF",
            "流通市值": 1100000000.0,
        }

        entity = task._parse_etf(raw)
        assert entity.fund_size == 1100000000.0

    def test_parse_etf_no_fund_size(self):
        task, db, source = self._make_task()

        raw = {
            "代码": "510050",
            "名称": "华夏上证50ETF",
        }

        entity = task._parse_etf(raw)
        assert entity.fund_size is None

    def test_infer_market_sh(self):
        task, db, source = self._make_task()
        assert task._infer_market("510050") == "SH"
        assert task._infer_market("588000") == "SH"

    def test_infer_market_sz(self):
        task, db, source = self._make_task()
        assert task._infer_market("159919") == "SZ"
        assert task._infer_market("399006") == "SZ"

    def test_infer_market_cn(self):
        task, db, source = self._make_task()
        assert task._infer_market("H00001") == "CN"

    def test_parse_float(self):
        task, db, source = self._make_task()
        assert task._parse_float(3.14) == 3.14
        assert task._parse_float("2.5") == 2.5
        assert task._parse_float(None) is None
        assert task._parse_float("invalid") is None

    def test_run_empty_result(self):
        task, db, source = self._make_task()
        source.get_etf_spot_list.return_value = []

        count = task.run()
        assert count == 0
        db.connection.assert_not_called()

    def test_run_with_data(self):
        task, db, source = self._make_task()
        source.get_etf_spot_list.return_value = [
            {"代码": "510050", "名称": "华夏上证50ETF"},
            {"代码": "510300", "名称": "华泰柏瑞沪深300ETF"},
        ]

        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        count = task.run()
        assert count == 2
        mock_cur.executemany.assert_called_once()

    def test_run_skips_invalid_items(self):
        task, db, source = self._make_task()
        source.get_etf_spot_list.return_value = [
            {"代码": "510050", "名称": "华夏上证50ETF"},
            {"名称": "无效数据"},
            {"代码": "510300", "名称": "华泰柏瑞沪深300ETF"},
        ]

        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        count = task.run()
        assert count == 2

    def test_upsert_sql_exists(self):
        sql = EtfInfoEntity.upsert_sql()
        assert "INSERT INTO etf_info" in sql
        assert "ON CONFLICT (etf_code)" in sql

    def test_entity_to_tuple(self):
        entity = EtfInfoEntity(
            etf_code="510050",
            etf_name="华夏上证50ETF",
            fund_size=1200000000.0,
            market="SH",
        )
        t = entity.to_upsert_tuple()
        assert t[0] == "510050"
        assert t[1] == "华夏上证50ETF"
        assert t[4] == 1200000000.0
        assert t[6] == "SH"
