package org.cwowhappy.securityanalyze.shared.infrastructure.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件服务配置属性。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private boolean enabled = false;
    private String host = "smtp.example.com";
    private int port = 587;
    private String username = "";
    private String password = "";
    private String from = "noreply@example.com";
}
