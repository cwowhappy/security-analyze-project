package org.cwowhappy.securityanalyze.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建股票请求 DTO。
 */
@Data
public class CreateStockRequest {

    @NotBlank(message = "股票编号不能为空")
    private String stockCode;

    @NotBlank(message = "股票名称不能为空")
    private String name;

    private String market;

    private String tsCode;

    private String fullName;

    private String exchange;

    private LocalDate listDate;

    private String industry;

    private String area;

    private Long totalShares;

    private Long floatShares;
}
