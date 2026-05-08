"""
Regression tests for 600938 中国海油采集失败问题。

根因分析：
1. @retry 装饰器被方法内部 try/except 完全屏蔽，网络波动时零重试
2. parse_date 不支持 YYYYMMDD 格式，EM 源上市时间解析为 None
3. get_company_detail/get_company_info_em 异常处理设计缺陷
"""
from unittest.mock import MagicMock, patch
import pandas as pd
import numpy as np
import pytest

from collector.sources.akshare_source import AkshareSource
from collector.utils import parse_date
from collector.tasks.company_task import CompanyTask


class TestRetryActuallyWorks:
    """验证 @retry 真正生效，而非被内部 try/except 屏蔽"""

    def test_get_company_detail_retries_on_network_error(self):
        """get_company_detail 应在网络异常时触发重试"""
        source = AkshareSource()
        call_count = 0

        def failing_then_success(*args, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise ConnectionError("network down")
            # 第3次成功
            return pd.DataFrame({"公司名称": ["中国海油"]})

        with patch.object(source._ak, "stock_profile_cninfo", side_effect=failing_then_success):
            result = source.get_company_detail("600938")

        assert call_count == 3, f"Expected 3 calls (2 retries + 1 success), got {call_count}"
        assert result is not None
        assert result["公司名称"] == "中国海油"

    def test_get_company_info_em_retries_on_network_error(self):
        """get_company_info_em 应在网络异常时触发重试"""
        source = AkshareSource()
        call_count = 0

        def failing_then_success(*args, **kwargs):
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise ConnectionError("network down")
            return pd.DataFrame({"item": ["股票简称"], "value": ["中国海油"]})

        with patch.object(source._ak, "stock_individual_info_em", side_effect=failing_then_success):
            result = source.get_company_info_em("600938")

        assert call_count == 3, f"Expected 3 calls, got {call_count}"
        assert result is not None
        assert result["股票简称"] == "中国海油"

    def test_returns_none_after_all_retries_exhausted(self):
        """重试全部失败后应返回 None（而非抛出异常）"""
        source = AkshareSource()

        with patch.object(source._ak, "stock_profile_cninfo", side_effect=ConnectionError("always fails")):
            result = source.get_company_detail("600938")

        assert result is None


class TestParseDateFormats:
    """验证 parse_date 支持多种常见日期格式"""

    def test_yyyy_mm_dd(self):
        assert parse_date("2022-04-21") == "2022-04-21"

    def test_yyyymmdd(self):
        """EM 源返回的格式，当前会失败"""
        assert parse_date("20220421") == "2022-04-21"

    def test_yyyy_mm_dd_with_space(self):
        assert parse_date("2022-04-21 00:00:00") == "2022-04-21"

    def test_datetime_object(self):
        from datetime import datetime
        dt = datetime(2022, 4, 21)
        assert parse_date(dt) == "2022-04-21"

    def test_none_and_empty(self):
        assert parse_date(None) is None
        assert parse_date("") is None

    def test_invalid_returns_none(self):
        assert parse_date("not-a-date") is None


class TestCompanyTaskHandlesMissingDetail:
    """验证 CompanyTask 在 detail 为 None 时仍能正确处理 EM fallback"""

    def _make_task(self):
        db = MagicMock()
        source = MagicMock()
        task = CompanyTask(db=db, source=source, monitor=None)
        return task, db, source

    def test_process_with_only_em_detail(self):
        """cninfo 失败但 EM 成功时，应使用 EM fallback 写入数据"""
        task, db, source = self._make_task()
        source.get_company_detail.return_value = None
        source.get_company_info_em.return_value = {
            "股票简称": "中国海油",
            "行业": "油气开采Ⅱ",
            "上市时间": "20220421",
        }
        db.fetchone.side_effect = [
            None,   # company 不存在
            None,   # security 不存在
        ]
        db.execute_returning.return_value = (42,)  # RETURNING id

        created, updated, failed = task._process_stocks([{"code": "600938", "name": ""}])

        assert failed == 0
        assert created + updated == 1
        # 验证 execute_returning 被调用于 INSERT company，且 company_name 正确
        db.execute_returning.assert_called_once()
        insert_call = db.execute_returning.call_args
        company_name = insert_call[0][1][0]
        assert company_name == "中国海油"

    def test_process_with_nan_in_detail(self):
        """cninfo 返回的数据中含 NaN 时不应导致异常"""
        task, db, source = self._make_task()
        source.get_company_detail.return_value = {
            "公司名称": np.nan,
            "A股简称": np.nan,
            "所属行业": "油气开采Ⅱ",
            "注册地址": "北京市",
            "成立日期": "2001-02-01",
            "注册资金": "4800000万元",
        }
        source.get_company_info_em.return_value = None
        db.fetchone.side_effect = [
            None,   # company 不存在
            None,   # security 不存在
        ]
        db.execute_returning.return_value = (42,)

        created, updated, failed = task._process_stocks([{"code": "600938", "name": ""}])

        assert failed == 0
        # 验证 company_name 已清理 NaN 并 fallback
        db.execute_returning.assert_called_once()
        insert_call = db.execute_returning.call_args
        company_name = insert_call[0][1][0]
        assert company_name != np.nan
        assert company_name is not None
