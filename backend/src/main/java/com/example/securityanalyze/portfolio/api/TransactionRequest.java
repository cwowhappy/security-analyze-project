package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.domain.TradeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {
    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @NotNull(message = "交易日期不能为空")
    private LocalDate tradeDate;

    @NotNull(message = "交易类型不能为空")
    private TradeType tradeType;

    private BigDecimal price;

    @NotNull(message = "成交股数不能为空")
    @PositiveOrZero(message = "成交股数不能为负数")
    private BigDecimal quantity;

    @PositiveOrZero(message = "交易费用不能为负数")
    private BigDecimal fee;

    @PositiveOrZero(message = "税费不能为负数")
    private BigDecimal tax;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
