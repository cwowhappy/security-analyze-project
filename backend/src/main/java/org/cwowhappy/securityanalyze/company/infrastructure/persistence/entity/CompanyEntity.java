package org.cwowhappy.securityanalyze.company.infrastructure.persistence.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JDBC 持久化实体，与数据库表 tb_company_basic 映射。
 */
@Data
public class CompanyEntity {

    private String id;
    private String unifiedSocialCreditCode;
    private String name;
    private String shortName;
    private String englishName;
    private String formerName;
    private String legalRepresentative;
    private String chairman;
    private String manager;
    private String secretary;
    private BigDecimal regCapital;
    private LocalDate setupDate;
    private String province;
    private String city;
    private String regAddress;
    private String officeAddress;
    private String website;
    private String industry;
    private String mainBusiness;
    private String businessScope;
    private String introduction;
    private Integer employees;
    private String controllerName;
    private String controllerType;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
