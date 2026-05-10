package org.cwowhappy.securityanalyze.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建公司请求 DTO。
 */
@Data
public class CreateCompanyRequest {

    private String unifiedSocialCreditCode;

    @NotBlank(message = "公司名称不能为空")
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
}
