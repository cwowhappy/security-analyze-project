package org.cwowhappy.securityanalyze.company.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公司领域实体（Aggregate Root）。
 * 纯 POJO，无框架依赖。
 */
@Getter
@Builder
public class Company {

    private final CompanyId id;
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
