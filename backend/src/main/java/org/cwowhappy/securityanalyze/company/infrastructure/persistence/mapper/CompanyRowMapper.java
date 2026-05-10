package org.cwowhappy.securityanalyze.company.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.company.infrastructure.persistence.entity.CompanyEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Company JDBC RowMapper。
 */
@Component
public class CompanyRowMapper implements RowMapper<CompanyEntity> {

    @Override
    public CompanyEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        CompanyEntity entity = new CompanyEntity();
        entity.setId(rs.getString("id"));
        entity.setUnifiedSocialCreditCode(rs.getString("unified_social_credit_code"));
        entity.setName(rs.getString("name"));
        entity.setShortName(rs.getString("short_name"));
        entity.setEnglishName(rs.getString("english_name"));
        entity.setFormerName(rs.getString("former_name"));
        entity.setLegalRepresentative(rs.getString("legal_representative"));
        entity.setChairman(rs.getString("chairman"));
        entity.setManager(rs.getString("manager"));
        entity.setSecretary(rs.getString("secretary"));
        entity.setRegCapital(rs.getBigDecimal("reg_capital"));
        java.sql.Date setupDate = rs.getDate("setup_date");
        entity.setSetupDate(setupDate != null ? setupDate.toLocalDate() : null);
        entity.setProvince(rs.getString("province"));
        entity.setCity(rs.getString("city"));
        entity.setRegAddress(rs.getString("reg_address"));
        entity.setOfficeAddress(rs.getString("office_address"));
        entity.setWebsite(rs.getString("website"));
        entity.setIndustry(rs.getString("industry"));
        entity.setMainBusiness(rs.getString("main_business"));
        entity.setBusinessScope(rs.getString("business_scope"));
        entity.setIntroduction(rs.getString("introduction"));
        entity.setEmployees(rs.getObject("employees", Integer.class));
        entity.setControllerName(rs.getString("controller_name"));
        entity.setControllerType(rs.getString("controller_type"));
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        entity.setCreatedAt(rs.getTimestamp("created_at") != null
                ? rs.getTimestamp("created_at").toLocalDateTime()
                : null);
        return entity;
    }
}
