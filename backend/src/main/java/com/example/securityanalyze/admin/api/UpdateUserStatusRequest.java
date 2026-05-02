package com.example.securityanalyze.admin.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
