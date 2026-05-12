package org.cwowhappy.securityanalyze.company.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.company.domain.model.Company;
import org.cwowhappy.securityanalyze.company.domain.model.CompanyId;
import org.cwowhappy.securityanalyze.company.domain.repository.CompanyRepository;
import org.cwowhappy.securityanalyze.company.infrastructure.persistence.entity.CompanyEntity;
import org.cwowhappy.securityanalyze.company.infrastructure.persistence.mapper.CompanyRowMapper;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 公司仓库 JDBC 实现（Adapter）。
 */
@Repository
@RequiredArgsConstructor
public class JdbcCompanyRepository implements CompanyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CompanyRowMapper rowMapper;

    @Override
    public Optional<Company> findById(CompanyId id) {
        String sql = "SELECT * FROM tb_company_basic WHERE id = :id";
        List<CompanyEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("id", id.getValue()), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<Company> findByUscCode(String uscCode) {
        String sql = "SELECT * FROM tb_company_basic WHERE unified_social_credit_code = :uscCode";
        List<CompanyEntity> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("uscCode", uscCode), rowMapper);
        return results.stream().findFirst().map(this::toDomain);
    }

    @Override
    public PageResult<Company> findByPage(PageQuery pageQuery) {
        return findByPage(pageQuery, null, null, null, null);
    }

    @Override
    public PageResult<Company> findByPage(PageQuery pageQuery, String industry, String province, String controllerType, String keyword) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(industry)) {
            conditions.add("industry = :industry");
            params.addValue("industry", industry);
        }
        if (StringUtils.hasText(province)) {
            conditions.add("province = :province");
            params.addValue("province", province);
        }
        if (StringUtils.hasText(controllerType)) {
            conditions.add("controller_type = :controllerType");
            params.addValue("controllerType", controllerType);
        }
        if (StringUtils.hasText(keyword)) {
            conditions.add("(name LIKE :keyword OR short_name LIKE :keyword OR unified_social_credit_code LIKE :keyword)");
            params.addValue("keyword", "%" + keyword + "%");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        // Count
        String countSql = "SELECT COUNT(*) FROM tb_company_basic" + where;
        Number count = jdbcTemplate.queryForObject(countSql, params, Number.class);
        long total = count != null ? count.longValue() : 0L;

        // Query
        int offset = (pageQuery.getPage() - 1) * pageQuery.getSize();
        params.addValue("limit", pageQuery.getSize());
        params.addValue("offset", offset);

        String querySql = "SELECT * FROM tb_company_basic" + where + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        List<Company> list = jdbcTemplate.query(querySql, params, rowMapper).stream()
                .map(this::toDomain)
                .toList();

        return PageResult.<Company>builder()
                .list(list)
                .total(total)
                .page(pageQuery.getPage())
                .size(pageQuery.getSize())
                .build();
    }

    @Override
    public List<Company> findByIndustry(String industry) {
        String sql = "SELECT * FROM tb_company_basic WHERE industry = :industry ORDER BY name";
        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("industry", industry), rowMapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Company> findByNameLike(String keyword) {
        String sql = "SELECT * FROM tb_company_basic WHERE name LIKE :keyword ORDER BY name";
        return jdbcTemplate.query(sql,
                new MapSqlParameterSource("keyword", "%" + keyword + "%"), rowMapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public CompanyId save(Company company) {
        String sql = """
                INSERT INTO tb_company_basic (
                    id, unified_social_credit_code, name, short_name, english_name, former_name,
                    legal_representative, chairman, manager, secretary, reg_capital, setup_date,
                    province, city, reg_address, office_address, website, industry,
                    main_business, business_scope, introduction, employees,
                    controller_name, controller_type, updated_at, created_at
                ) VALUES (
                    :id, :unifiedSocialCreditCode, :name, :shortName, :englishName, :formerName,
                    :legalRepresentative, :chairman, :manager, :secretary, :regCapital, :setupDate,
                    :province, :city, :regAddress, :officeAddress, :website, :industry,
                    :mainBusiness, :businessScope, :introduction, :employees,
                    :controllerName, :controllerType, :updatedAt, :createdAt
                )
                ON CONFLICT (id) DO UPDATE SET
                    unified_social_credit_code = EXCLUDED.unified_social_credit_code,
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
                    updated_at = EXCLUDED.updated_at,
                    created_at = EXCLUDED.created_at
                """;
        CompanyEntity entity = toEntity(company);
        SqlParameterSource params = new BeanPropertySqlParameterSource(entity);
        jdbcTemplate.update(sql, params);
        return company.getId();
    }

    @Override
    public void deleteById(CompanyId id) {
        String sql = "DELETE FROM tb_company_basic WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id.getValue()));
    }

    private Company toDomain(CompanyEntity entity) {
        return Company.builder()
                .id(CompanyId.of(entity.getId()))
                .unifiedSocialCreditCode(entity.getUnifiedSocialCreditCode())
                .name(entity.getName())
                .shortName(entity.getShortName())
                .englishName(entity.getEnglishName())
                .formerName(entity.getFormerName())
                .legalRepresentative(entity.getLegalRepresentative())
                .chairman(entity.getChairman())
                .manager(entity.getManager())
                .secretary(entity.getSecretary())
                .regCapital(entity.getRegCapital())
                .setupDate(entity.getSetupDate())
                .province(entity.getProvince())
                .city(entity.getCity())
                .regAddress(entity.getRegAddress())
                .officeAddress(entity.getOfficeAddress())
                .website(entity.getWebsite())
                .industry(entity.getIndustry())
                .mainBusiness(entity.getMainBusiness())
                .businessScope(entity.getBusinessScope())
                .introduction(entity.getIntroduction())
                .employees(entity.getEmployees())
                .controllerName(entity.getControllerName())
                .controllerType(entity.getControllerType())
                .updatedAt(entity.getUpdatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private CompanyEntity toEntity(Company company) {
        CompanyEntity entity = new CompanyEntity();
        entity.setId(company.getId().getValue());
        entity.setUnifiedSocialCreditCode(company.getUnifiedSocialCreditCode());
        entity.setName(company.getName());
        entity.setShortName(company.getShortName());
        entity.setEnglishName(company.getEnglishName());
        entity.setFormerName(company.getFormerName());
        entity.setLegalRepresentative(company.getLegalRepresentative());
        entity.setChairman(company.getChairman());
        entity.setManager(company.getManager());
        entity.setSecretary(company.getSecretary());
        entity.setRegCapital(company.getRegCapital());
        entity.setSetupDate(company.getSetupDate());
        entity.setProvince(company.getProvince());
        entity.setCity(company.getCity());
        entity.setRegAddress(company.getRegAddress());
        entity.setOfficeAddress(company.getOfficeAddress());
        entity.setWebsite(company.getWebsite());
        entity.setIndustry(company.getIndustry());
        entity.setMainBusiness(company.getMainBusiness());
        entity.setBusinessScope(company.getBusinessScope());
        entity.setIntroduction(company.getIntroduction());
        entity.setEmployees(company.getEmployees());
        entity.setControllerName(company.getControllerName());
        entity.setControllerType(company.getControllerType());
        entity.setUpdatedAt(company.getUpdatedAt());
        entity.setCreatedAt(company.getCreatedAt());
        return entity;
    }
}
