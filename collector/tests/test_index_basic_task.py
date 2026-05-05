"""Tests for IndexBasicTask."""
from unittest.mock import MagicMock
import pytest

from collector.db.postgres import PostgresDB
from collector.sources.akshare_source import AkshareSource
from collector.tasks.index_basic_task import IndexBasicTask
from collector.models import IndexInfoEntity


class TestIndexBasicTask:
    def _make_task(self):
        db = MagicMock(spec=PostgresDB)
        source = MagicMock(spec=AkshareSource)
        task = IndexBasicTask(db=db, source=source)
        return task, db, source

    def test_parse_index_success(self):
        task, db, source = self._make_task()

        raw = {
            "index_code": "000001",
            "display_name": " 上证指数 ",
            "publish_date": "1991-07-15",
        }

        entity = task._parse_index(raw)

        assert entity is not None
        assert entity.index_code == "000001"
        assert entity.index_name == "上证指数"
        assert entity.index_type == "宽基"
        assert entity.market == "SH"
        assert entity.publish_date == "1991-07-15"

    def test_parse_index_missing_code(self):
        task, db, source = self._make_task()

        raw = {"display_name": "上证指数"}
        assert task._parse_index(raw) is None

    def test_parse_index_missing_name(self):
        task, db, source = self._make_task()

        raw = {"index_code": "000001"}
        assert task._parse_index(raw) is None

    def test_parse_index_empty_publish_date(self):
        task, db, source = self._make_task()

        raw = {
            "index_code": "000001",
            "display_name": "上证指数",
            "publish_date": "",
        }
        entity = task._parse_index(raw)
        assert entity.publish_date is None

    def test_parse_index_nan_publish_date(self):
        task, db, source = self._make_task()

        raw = {
            "index_code": "000001",
            "display_name": "上证指数",
            "publish_date": "nan",
        }
        entity = task._parse_index(raw)
        assert entity.publish_date is None

    def test_infer_index_type_wide(self):
        task, db, source = self._make_task()
        assert task._infer_index_type("000001", "上证指数") == "宽基"
        assert task._infer_index_type("000300", "沪深300") == "宽基"
        assert task._infer_index_type("000905", "中证500") == "宽基"

    def test_infer_index_type_industry(self):
        task, db, source = self._make_task()
        assert task._infer_index_type("399989", "中证医疗行业指数") == "行业"
        assert task._infer_index_type("399989", "中证医疗产业指数") == "行业"

    def test_infer_index_type_theme(self):
        task, db, source = self._make_task()
        assert task._infer_index_type("000021", "上证180金融主题指数") == "主题"
        assert task._infer_index_type("000021", "上证180金融概念指数") == "主题"

    def test_infer_index_type_strategy(self):
        task, db, source = self._make_task()
        assert task._infer_index_type("000001", "某某策略指数") == "策略"

    def test_infer_index_type_other(self):
        task, db, source = self._make_task()
        assert task._infer_index_type("H30035", "恒生某指数") == "其他"

    def test_infer_market_sh(self):
        task, db, source = self._make_task()
        assert task._infer_market("000001") == "SH"
        assert task._infer_market("999999") == "SH"

    def test_infer_market_sz(self):
        task, db, source = self._make_task()
        assert task._infer_market("399001") == "SZ"
        assert task._infer_market("399006") == "SZ"
        assert task._infer_market("880001") == "SZ"

    def test_infer_market_cn(self):
        task, db, source = self._make_task()
        assert task._infer_market("H30035") == "CN"

    def test_run_empty_result(self):
        task, db, source = self._make_task()
        source.get_index_list.return_value = []

        count = task.run()
        assert count == 0
        db.connection.assert_not_called()

    def test_run_with_data(self):
        task, db, source = self._make_task()
        source.get_index_list.return_value = [
            {"index_code": "000001", "display_name": "上证指数"},
            {"index_code": "399001", "display_name": "深证成指"},
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
        source.get_index_list.return_value = [
            {"index_code": "000001", "display_name": "上证指数"},
            {"display_name": "无效数据"},  # 缺少 index_code
            {"index_code": "399001", "display_name": "深证成指"},
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
        sql = IndexInfoEntity.upsert_sql()
        assert "INSERT INTO index_info" in sql
        assert "ON CONFLICT (index_code)" in sql

    def test_entity_to_tuple(self):
        entity = IndexInfoEntity(
            index_code="000001",
            index_name="上证指数",
            index_type="宽基",
            market="SH",
            publish_date="1991-07-15",
        )
        t = entity.to_upsert_tuple()
        assert t[0] == "000001"
        assert t[1] == "上证指数"
        assert t[2] == "宽基"
