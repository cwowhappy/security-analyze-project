package com.example.securityanalyze.research.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DCF 估值计算响应 DTO
 */
@Data
public class DcfResponse {

    /** 每股公允价 */
    private BigDecimal fairPrice;

    /** 公允价区间下限（敏感性分析） */
    private BigDecimal fairPriceRangeLow;

    /** 公允价区间上限（敏感性分析） */
    private BigDecimal fairPriceRangeHigh;

    /** 相对当前价涨跌百分比（正数表示低估，负数表示高估） */
    private BigDecimal upsidePercent;

    /** 实际使用的假设参数 */
    private DcfRequest appliedAssumptions;
}
