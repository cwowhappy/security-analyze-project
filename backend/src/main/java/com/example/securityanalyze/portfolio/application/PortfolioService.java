package com.example.securityanalyze.portfolio.application;

import com.example.securityanalyze.portfolio.api.PortfolioAccessDeniedException;
import com.example.securityanalyze.portfolio.api.PortfolioNotFoundException;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioRepository;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import com.example.securityanalyze.portfolio.domain.Position;
import com.example.securityanalyze.portfolio.domain.PositionRepository;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Portfolio createPortfolio(String username, String name, PortfolioType type, String broker, String description) {
        User user = getUser(username);
        Portfolio portfolio = new Portfolio();
        portfolio.setUserId(user.getId());
        portfolio.setName(name);
        portfolio.setType(type);
        portfolio.setBroker(broker);
        portfolio.setDescription(description);
        return portfolioRepository.save(portfolio);
    }

    @Transactional(readOnly = true)
    public List<Portfolio> listPortfolios(String username) {
        User user = getUser(username);
        return portfolioRepository.findByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public Portfolio getPortfolio(String username, Long portfolioId) {
        User user = getUser(username);
        return portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
                .orElseThrow(() -> new PortfolioNotFoundException("组合不存在"));
    }

    @Transactional
    public void updatePortfolio(String username, Long portfolioId, String name, PortfolioType type, String broker, String description) {
        User user = getUser(username);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
                .orElseThrow(() -> new PortfolioNotFoundException("组合不存在"));
        portfolio.setName(name);
        if (type != null) {
            portfolio.setType(type);
        }
        portfolio.setBroker(broker);
        portfolio.setDescription(description);
        portfolioRepository.update(portfolio);
    }

    @Transactional
    public void deletePortfolio(String username, Long portfolioId) {
        User user = getUser(username);
        portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
                .orElseThrow(() -> new PortfolioNotFoundException("组合不存在"));
        portfolioRepository.softDeleteByUserId(portfolioId, user.getId());
        log.info("删除组合, portfolioId={}, userId={}", portfolioId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<Position> listPositions(Long portfolioId) {
        return positionRepository.findByPortfolioId(portfolioId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPositionsWithQuote(Long portfolioId) {
        return positionRepository.findByPortfolioIdWithQuote(portfolioId);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new PortfolioAccessDeniedException("用户不存在"));
    }
}
