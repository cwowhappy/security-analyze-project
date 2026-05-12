"""PostgreSQL 公司仓库实现。"""

from collections.abc import Sequence

import structlog
import ulid

from data_collector.core.domain.company import Company
from data_collector.infrastructure.db import execute_query, execute_update

logger = structlog.get_logger(__name__)


class DbCompanyRepository:
    """基于 PostgreSQL 的公司仓库实现。"""

    def save(self, company: Company) -> None:
        """保存或更新公司数据（Upsert 语义）。"""
        if company.id is None:
            company.id = str(ulid.ULID())

        sql = """
        INSERT INTO tb_company_basic (
            id, unified_social_credit_code, name, short_name, english_name, former_name,
            legal_representative, chairman, manager, secretary, reg_capital, setup_date,
            province, city, reg_address, office_address, website, industry,
            main_business, business_scope, introduction, employees,
            controller_name, controller_type, created_at, updated_at
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s,
            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW()
        )
        ON CONFLICT (unified_social_credit_code) DO UPDATE SET
            name = EXCLUDED.name,
            short_name = EXCLUDED.short_name,
            english_name = EXCLUDED.english_name,
            former_name = EXCLUDED.former_name,
            legal_representative = EXCLUDED.legal_representative,
            chairman = EXCLUDED.chairman,
            manager = EXCLUDED.manager,
            secretary = EXCLUDED.secretary,
            reg_capital = EXCLUDED.reg_capital,
            setup_date = EXCLUDED.setup_date,
            province = EXCLUDED.province,
            city = EXCLUDED.city,
            reg_address = EXCLUDED.reg_address,
            office_address = EXCLUDED.office_address,
            website = EXCLUDED.website,
            industry = EXCLUDED.industry,
            main_business = EXCLUDED.main_business,
            business_scope = EXCLUDED.business_scope,
            introduction = EXCLUDED.introduction,
            employees = EXCLUDED.employees,
            controller_name = EXCLUDED.controller_name,
            controller_type = EXCLUDED.controller_type,
            updated_at = NOW()
        """
        params = (
            company.id,
            company.unified_social_credit_code,
            company.name,
            company.short_name,
            company.english_name,
            company.former_name,
            company.legal_representative,
            company.chairman,
            company.manager,
            company.secretary,
            company.reg_capital,
            company.setup_date,
            company.province,
            company.city,
            company.reg_address,
            company.office_address,
            company.website,
            company.industry,
            company.main_business,
            company.business_scope,
            company.introduction,
            company.employees,
            company.controller_name,
            company.controller_type,
        )
        execute_update(sql, params)
        logger.debug("公司已保存", name=company.name)

    def save_all(self, companies: Sequence[Company]) -> tuple[int, int]:
        """批量保存公司，返回 (成功数, 失败数)。"""
        success = 0
        failed = 0
        for company in companies:
            try:
                self.save(company)
                success += 1
            except Exception as e:
                logger.warning(
                    "批量保存公司失败",
                    name=company.name,
                    error=str(e),
                )
                failed += 1
        logger.info("批量保存完成", total=len(companies), success=success, failed=failed)
        return success, failed

    def find_by_usc_code(self, usc_code: str) -> Company | None:
        """根据统一社会信用代码查询。"""
        sql = """
        SELECT * FROM tb_company_basic WHERE unified_social_credit_code = %s
        """
        rows = execute_query(sql, (usc_code,))
        if not rows:
            return None
        return Company.from_dict(rows[0])

    def find_all(self) -> Sequence[Company]:
        """查询所有公司。"""
        sql = "SELECT * FROM tb_company_basic ORDER BY name"
        rows = execute_query(sql)
        return [Company.from_dict(row) for row in rows]

    def count(self) -> int:
        """返回公司总数。"""
        sql = "SELECT COUNT(*) as cnt FROM tb_company_basic"
        rows = execute_query(sql)
        return rows[0]["cnt"] if rows else 0
