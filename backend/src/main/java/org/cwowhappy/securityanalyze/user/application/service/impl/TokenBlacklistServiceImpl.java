package org.cwowhappy.securityanalyze.user.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.user.application.service.TokenBlacklistService;
import org.cwowhappy.securityanalyze.user.domain.repository.TokenSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Token 黑名单应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenSessionRepository tokenSessionRepository;

    @Override
    @Transactional
    public void recordToken(String userId, String token, LocalDateTime expiresAt) {
        String tokenHash = hashToken(token);
        tokenSessionRepository.save(userId, tokenHash, expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        String tokenHash = hashToken(token);
        return tokenSessionRepository.existsByTokenHash(tokenHash);
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        String tokenHash = hashToken(token);
        int deleted = tokenSessionRepository.deleteByTokenHash(tokenHash);
        log.info("Token 已吊销: deleted={}", deleted);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
