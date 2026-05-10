package org.cwowhappy.securityanalyze.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建股票请求 DTO。
 */
@Data
public class CreateStockRequest {

    @NotBlank(message = "股票代码不能为空")
    private String symbol;

    @NotBlank(message = "股票名称不能为空")
    private String name;

    private String market;

    @NotNull(message = "当前价格不能为空")
    @PositiveOrZero(message = "当前价格不能为负数")
    private BigDecimal currentPrice;

    private BigDecimal changePercent;
}
