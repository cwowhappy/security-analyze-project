package org.cwowhappy.securityanalyze.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * JWT Token 提供者，负责生成与验证 Token。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateToken(String userId, String username, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.getExpirationMs());

        return JWT.create()
                .withSubject(userId)
                .withClaim("username", username)
                .withClaim("role", role)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiration))
                .sign(Algorithm.HMAC256(jwtProperties.getSecret()));
    }

    public Optional<DecodedJWT> validateToken(String token) {
        try {
            return Optional.of(JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .build()
                    .verify(token));
        } catch (JWTVerificationException ex) {
            log.warn("JWT 验证失败: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public String getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        return decodedJWT.getSubject();
    }

    public Map<String, Claim> getClaimsFromToken(String token) {
        return JWT.decode(token).getClaims();
    }
}
