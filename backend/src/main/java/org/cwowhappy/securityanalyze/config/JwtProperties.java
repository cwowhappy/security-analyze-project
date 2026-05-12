package org.cwowhappy.securityanalyze.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "security-analyze-default-secret-key-change-in-production";
    private long expirationMs = 86400000;
}
