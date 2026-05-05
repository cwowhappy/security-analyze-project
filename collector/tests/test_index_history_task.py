"""Tests for IndexHistoryTask."""
from unittest.mock import MagicMock, patch
import pytest
import pandas as pd

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.index_history_task import IndexHistoryTask, GRANULARITY_MAP
from collector.models import IndexHistoryEntity


class TestIndexHistoryTask:
    def _make_task(self):
        db = MagicMock(spec=PostgresDB)
        source = MagicMock(spec=AkshareSource)
        task = IndexHistoryTask(db=db, source=source, max_workers=1)
        return task, db, source

    def test_granularity_map(self):
        assert GRANULARITY_MAP["day"] == "daily"
        assert GRANULARITY_MAP["week"] == "weekly"
        assert GRANULARITY_MAP["month"] == "monthly"

    def test_parse_history_df_success(self):
        task, db, source = self._make_task()

        df = pd.DataFrame({
            "日期": ["2024-01-01", "2024-01-02"],
            "开盘": [3000.0, 3050.0],
            "收盘": [3050.0, 3100.0],
            "最高": [3100.0, 3150.0],
            "最低": [2950.0, 3000.0],
            "成交量": [1000000, 1200000],
            "成交额": [500000000.0, 600000000.0],
            "振幅": [3.33, 3.50],
            "涨跌幅": [1.67, 1.64],
            "涨跌额": [50.0, 50.0],
            "换手率": [0.50, 0.55],
        })

        entities = task._parse_history_df("000001", "day", df)

        assert len(entities) == 2
        assert entities[0].index_code == "000001"
        assert entities[0].trade_date == "2024-01-01"
        assert entities[0].granularity == "day"
        assert entities[0].open_price == 3000.0
        assert entities[0].close_price == 3050.0
        assert entities[0].volume == 1000000

    def test_parse_history_df_empty(self):
        task, db, source = self._make_task()
        entities = task._parse_history_df("000001", "day", pd.DataFrame())
        assert len(entities) == 0

    def test_parse_history_df_with_nan(self):
        task, db, source = self._make_task()

        df = pd.DataFrame({
            "日期": ["2024-01-01", None],
            "开盘": [3000.0, float("nan")],
            "收盘": [3050.0, 3100.0],
            "最高": [3100.0, 3150.0],
            "最低": [2950.0, 3000.0],
            "成交量": [1000000, 1200000],
            "成交额": [500000000.0, float("nan")],
            "振幅": [3.33, 3.50],
            "涨跌幅": [1.67, 1.64],
            "涨跌额": [50.0, 50.0],
            "换手率": [0.50, 0.55],
        })

        entities = task._parse_history_df("000001", "day", df)

        # 第一行有效，第二行日期为 None 应被跳过
        assert len(entities) == 1
        assert entities[0].open_price == 3000.0

    def test_to_float(self):
        task, db, source = self._make_task()
        assert task._to_float(3.14) == 3.14
        assert task._to_float("2.5") == 2.5
        assert task._to_float(None) is None
        assert task._to_float(float("nan")) is None
        assert task._to_float("invalid") is None

    def test_to_int(self):
        task, db, source = self._make_task()
        assert task._to_int(100) == 100
        assert task._to_int("200") == 200
        assert task._to_int(150.7) == 150
        assert task._to_int(None) is None
        assert task._to_int(float("nan")) is None
        assert task._to_int("invalid") is None

    def test_collect_single_with_data(self):
        task, db, source = self._make_task()

        df = pd.DataFrame({
            "日期": ["2024-01-01"],
            "开盘": [3000.0],
            "收盘": [3050.0],
            "最高": [3100.0],
            "最低": [2950.0],
            "成交量": [1000000],
            "成交额": [500000000.0],
            "振幅": [3.33],
            "涨跌幅": [1.67],
            "涨跌额": [50.0],
            "换手率": [0.50],
        })
        source.get_index_history.return_value = df

        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        rows = task._collect_single("000001", "day", None, None, "test-session")

        assert rows == 1
        source.get_index_history.assert_called_once_with("000001", period="daily", start_date=None, end_date=None)
        mock_cur.executemany.assert_called_once()
        mock_cur.execute.assert_called_once()  # _mark_success

    def test_collect_single_no_data(self):
        task, db, source = self._make_task()
        source.get_index_history.return_value = None

        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        rows = task._collect_single("000001", "day", None, None, "test-session")

        assert rows == 0
        mock_cur.executemany.assert_not_called()
        mock_cur.execute.assert_called_once()  # _mark_success with 0 rows

    def test_load_success_set(self):
        task, db, source = self._make_task()

        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.fetchall.return_value = [
            ("000001#day",),
            ("000001#week",),
            ("399001#day",),
        ]
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        success_set = task._load_success_set("test-session")

        assert "000001#day" in success_set
        assert "000001#week" in success_set
        assert "399001#day" in success_set
        assert "000001#month" not in success_set

    def test_load_success_set_db_error(self):
        task, db, source = self._make_task()
        db.connection.side_effect = Exception("db error")

        success_set = task._load_success_set("test-session")
        assert success_set == set()

    def test_mark_success(self):
        task, db, source = self._make_task()

        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)
        mock_conn.cursor.return_value.__enter__ = MagicMock(return_value=mock_cur)
        mock_conn.cursor.return_value.__exit__ = MagicMock(return_value=False)
        db.connection.return_value = mock_conn

        task._mark_success("test-session", "000001", "day", 100)

        mock_cur.execute.assert_called_once()
        call_args = mock_cur.execute.call_args[0]
        assert call_args[1][0] == "test-session"
        assert call_args[1][1] == "000001#day"
        assert call_args[1][2] == 100

    def test_entity_upsert_sql(self):
        sql = IndexHistoryEntity.upsert_sql()
        assert "INSERT INTO index_history" in sql
        assert "ON CONFLICT (index_code, trade_date, granularity)" in sql

    def test_entity_to_tuple(self):
        entity = IndexHistoryEntity(
            index_code="000001",
            trade_date="2024-01-01",
            granularity="day",
            open_price=3000.0,
            close_price=3050.0,
            volume=1000000,
        )
        t = entity.to_upsert_tuple()
        assert t[0] == "000001"
        assert t[1] == "2024-01-01"
        assert t[2] == "day"
        assert t[3] == 3000.0
