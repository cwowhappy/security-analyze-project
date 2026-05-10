"""公司领域模型单元测试。"""

import pytest

from data_collector.core.domain.company import Company


class TestCompany:
    """公司领域模型测试。"""

    def test_should_create_company_with_valid_data(self) -> None:
        company = Company(name="平安银行股份有限公司")
        assert company.name == "平安银行股份有限公司"

    def test_should_raise_error_when_name_empty(self) -> None:
        with pytest.raises(ValueError, match="公司名称 name 不能为空"):
            Company(name="")

    def test_should_convert_to_dict_and_back(self) -> None:
        company = Company(
            name="平安银行股份有限公司",
            unified_social_credit_code="9144030019218537XX",
            legal_representative="谢永林",
            province="广东省",
            city="深圳市",
        )
        data = company.to_dict()
        restored = Company.from_dict(data)
        assert restored.name == "平安银行股份有限公司"
        assert restored.unified_social_credit_code == "9144030019218537XX"
