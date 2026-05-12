package org.cwowhappy.securityanalyze.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮箱验证请求 DTO。
 */
@Data
public class VerifyEmailRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
