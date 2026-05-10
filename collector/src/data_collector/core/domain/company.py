"""公司领域模型，与 tb_company_basic 表结构对应。"""

from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal


@dataclass
class Company:
    """公司基础信息领域实体。"""

    id: str | None = None
    unified_social_credit_code: str | None = None
    name: str = ""
    short_name: str | None = None
    english_name: str | None = None
    former_name: str | None = None
    legal_representative: str | None = None
    chairman: str | None = None
    manager: str | None = None
    secretary: str | None = None
    reg_capital: Decimal | None = None
    setup_date: date | None = None
    province: str | None = None
    city: str | None = None
    reg_address: str | None = None
    office_address: str | None = None
    website: str | None = None
    industry: str | None = None
    main_business: str | None = None
    business_scope: str | None = None
    introduction: str | None = None
    employees: int | None = None
    controller_name: str | None = None
    controller_type: str | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None

    def __post_init__(self) -> None:
        if not self.name:
            raise ValueError("公司名称 name 不能为空")

    def to_dict(self) -> dict:
        """转换为字典（用于入库）。"""
        return {
            "id": self.id,
            "unified_social_credit_code": self.unified_social_credit_code,
            "name": self.name,
            "short_name": self.short_name,
            "english_name": self.english_name,
            "former_name": self.former_name,
            "legal_representative": self.legal_representative,
            "chairman": self.chairman,
            "manager": self.manager,
            "secretary": self.secretary,
            "reg_capital": self.reg_capital,
            "setup_date": self.setup_date,
            "province": self.province,
            "city": self.city,
            "reg_address": self.reg_address,
            "office_address": self.office_address,
            "website": self.website,
            "industry": self.industry,
            "main_business": self.main_business,
            "business_scope": self.business_scope,
            "introduction": self.introduction,
            "employees": self.employees,
            "controller_name": self.controller_name,
            "controller_type": self.controller_type,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "Company":
        """从字典创建实例（从数据库读取）。"""
        return cls(
            id=data.get("id"),
            unified_social_credit_code=data.get("unified_social_credit_code"),
            name=data.get("name", ""),
            short_name=data.get("short_name"),
            english_name=data.get("english_name"),
            former_name=data.get("former_name"),
            legal_representative=data.get("legal_representative"),
            chairman=data.get("chairman"),
            manager=data.get("manager"),
            secretary=data.get("secretary"),
            reg_capital=data.get("reg_capital"),
            setup_date=data.get("setup_date"),
            province=data.get("province"),
            city=data.get("city"),
            reg_address=data.get("reg_address"),
            office_address=data.get("office_address"),
            website=data.get("website"),
            industry=data.get("industry"),
            main_business=data.get("main_business"),
            business_scope=data.get("business_scope"),
            introduction=data.get("introduction"),
            employees=data.get("employees"),
            controller_name=data.get("controller_name"),
            controller_type=data.get("controller_type"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )
