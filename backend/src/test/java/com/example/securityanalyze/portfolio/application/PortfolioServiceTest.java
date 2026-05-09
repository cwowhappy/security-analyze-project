package com.example.securityanalyze.portfolio.application;

import com.example.securityanalyze.portfolio.domain.PortfolioAccessDeniedException;
import com.example.securityanalyze.portfolio.domain.PortfolioNotFoundException;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioRepository;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setStatus(UserStatus.APPROVED);
        return user;
    }

    @Test
    void shouldCreatePortfolio() {
        User user = mockUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> {
            Portfolio p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Portfolio result = portfolioService.createPortfolio("testuser", "组合1", PortfolioType.REAL, "华泰", "备注");

        assertEquals("组合1", result.getName());
        assertEquals(PortfolioType.REAL, result.getType());
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    void shouldGetPortfolio() {
        User user = mockUser();
        Portfolio p = new Portfolio();
        p.setId(1L);
        p.setUserId(1L);
        p.setName("组合1");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(p));

        Portfolio result = portfolioService.getPortfolio("testuser", 1L);
        assertEquals("组合1", result.getName());
    }

    @Test
    void shouldThrowWhenPortfolioNotFound() {
        User user = mockUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class, () -> portfolioService.getPortfolio("testuser", 1L));
    }

    @Test
    void shouldListPortfolios() {
        User user = mockUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.findByUserId(1L)).thenReturn(List.of(new Portfolio()));

        List<Portfolio> result = portfolioService.listPortfolios("testuser");
        assertEquals(1, result.size());
    }

    @Test
    void shouldUpdatePortfolio() {
        User user = mockUser();
        Portfolio p = new Portfolio();
        p.setId(1L);
        p.setUserId(1L);
        p.setName("旧名称");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(p));

        portfolioService.updatePortfolio("testuser", 1L, "新名称", null, "新券商", null);

        verify(portfolioRepository).update(argThat(arg -> arg.getName().equals("新名称") && arg.getBroker().equals("新券商")));
    }

    @Test
    void shouldDeletePortfolio() {
        User user = mockUser();
        Portfolio p = new Portfolio();
        p.setId(1L);
        p.setUserId(1L);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(p));

        portfolioService.deletePortfolio("testuser", 1L);

        verify(portfolioRepository).softDeleteByUserId(1L, 1L);
    }
}
