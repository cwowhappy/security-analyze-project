package org.cwowhappy.securityanalyze.config;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT Token 提供者单元测试。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-12345678901234567890");
        jwtProperties.setExpirationMs(3600000);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    void shouldGenerateTokenWithCorrectClaims() {
        // Act
        String token = jwtTokenProvider.generateToken("user001", "testuser", "viewer");

        // Assert
        assertThat(token).isNotBlank();
        Optional<DecodedJWT> decoded = jwtTokenProvider.validateToken(token);
        assertThat(decoded).isPresent();
        assertThat(decoded.get().getSubject()).isEqualTo("user001");
        assertThat(decoded.get().getClaim("username").asString()).isEqualTo("testuser");
        assertThat(decoded.get().getClaim("role").asString()).isEqualTo("viewer");
    }

    @Test
    void shouldValidateGeneratedTokenSuccessfully() {
        // Arrange
        String token = jwtTokenProvider.generateToken("user001", "testuser", "viewer");

        // Act
        Optional<DecodedJWT> result = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(result).isPresent();
    }

    @Test
    void shouldReturnEmptyWhenTokenInvalid() {
        // Act
        Optional<DecodedJWT> result = jwtTokenProvider.validateToken("invalid.token.here");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenTokenTampered() {
        // Arrange
        String token = jwtTokenProvider.generateToken("user001", "testuser", "viewer");
        String tampered = token.substring(0, token.length() - 5) + "xxxxx";

        // Act
        Optional<DecodedJWT> result = jwtTokenProvider.validateToken(tampered);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenTokenSignedWithDifferentSecret() {
        // Arrange
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("different-secret-key-123456789012345");
        otherProps.setExpirationMs(3600000);
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);
        String token = otherProvider.generateToken("user001", "testuser", "viewer");

        // Act
        Optional<DecodedJWT> result = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldExtractUserIdFromToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken("user001", "testuser", "viewer");

        // Act
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertThat(userId).isEqualTo("user001");
    }

    @Test
    void shouldExtractClaimsFromToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken("user001", "testuser", "viewer");

        // Act
        Map<String, Claim> claims = jwtTokenProvider.getClaimsFromToken(token);

        // Assert
        assertThat(claims).containsKey("username");
        assertThat(claims.get("username").asString()).isEqualTo("testuser");
        assertThat(claims).containsKey("role");
        assertThat(claims.get("role").asString()).isEqualTo("viewer");
    }
}
