package org.cwowhappy.securityanalyze.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 公司简要信息 DTO（跨模块共享）。
 */
@Data
@Builder
public class CompanyBriefDTO {

    private String id;
    private String unifiedSocialCreditCode;
    private String name;
    private String shortName;
    private String legalRepresentative;
    private BigDecimal regCapital;
    private LocalDate setupDate;
    private String mainBusiness;
}
