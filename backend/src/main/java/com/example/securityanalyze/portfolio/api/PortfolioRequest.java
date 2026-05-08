package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.domain.PortfolioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortfolioRequest {
    @NotBlank(message = "组合名称不能为空")
    @Size(max = 100, message = "组合名称长度不能超过100")
    private String name;

    @NotNull(message = "组合类型不能为空")
    private PortfolioType type;

    @Size(max = 100, message = "券商名称长度不能超过100")
    private String broker;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;
}
