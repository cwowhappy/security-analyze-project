package org.cwowhappy.securityanalyze.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求 DTO。
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "重置令牌不能为空")
    private String token;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码至少 8 位")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
