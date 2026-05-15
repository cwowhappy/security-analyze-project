from unittest.mock import patch

import pandas as pd
import pytest

from data_collector.adapters.company_info_akshare_adapter import CompanyInfoAkshareAdapter
from data_collector.core.config.field_mapping_config import SourceConfig


class TestCompanyInfoAkshareAdapter:
    def test_fetch_success(self):
        adapter = CompanyInfoAkshareAdapter()
        mock_df = pd.DataFrame(
            [
                {
                    "公司名称": "平安银行股份有限公司",
                    "A股简称": "平安银行",
                    "英文名称": "Ping An Bank Co., Ltd.",
                    "曾用简称": "深发展A",
                    "法人代表": "谢永林",
                    "注册资金": "1,940,592.75万",
                    "成立日期": "1987-12-22",
                    "注册地址": "广东省深圳市罗湖区深南东路5047号",
                    "办公地址": "广东省深圳市福田区益田路5033号平安金融中心",
                    "官方网站": "http://bank.pingan.com",
                    "所属行业": "货币金融服务",
                    "主营业务": "吸收公众存款；发放短期、中期和长期贷款等",
                    "经营范围": "办理人民币存、贷、结算、汇兑业务等",
                    "机构简介": "平安银行是一家总部设在深圳的全国性股份制商业银行",
                }
            ]
        )
        with patch("akshare.stock_profile_cninfo", return_value=mock_df):
            result = adapter.fetch(
                "000001",
                SourceConfig(name="akshare", adapter="company_info_akshare_adapter", priority=1),
            )
        assert result["公司名称"] == "平安银行股份有限公司"
        assert result["注册资金"] == "1,940,592.75万"
        assert result["成立日期"] == "1987-12-22"

    def test_fetch_empty_raises(self):
        adapter = CompanyInfoAkshareAdapter()
        with patch("akshare.stock_profile_cninfo", return_value=pd.DataFrame()):
            with pytest.raises(ValueError, match="未找到公司信息"):
                adapter.fetch(
                    "000001",
                    SourceConfig(name="akshare", adapter="company_info_akshare_adapter", priority=1),
                )

    def test_fetch_none_raises(self):
        adapter = CompanyInfoAkshareAdapter()
        with patch("akshare.stock_profile_cninfo", return_value=None):
            with pytest.raises(ValueError, match="未找到公司信息"):
                adapter.fetch(
                    "000001",
                    SourceConfig(name="akshare", adapter="company_info_akshare_adapter", priority=1),
                )
