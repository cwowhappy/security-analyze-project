package com.example.securityanalyze.portfolio.application;

import com.example.securityanalyze.portfolio.domain.InsufficientPositionException;
import com.example.securityanalyze.portfolio.domain.PortfolioNotFoundException;
import com.example.securityanalyze.portfolio.domain.TransactionNotFoundException;
import com.example.securityanalyze.portfolio.domain.*;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PositionCalculationService calculationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setStatus(UserStatus.APPROVED);
        return user;
    }

    private Position mockPosition(BigDecimal qty) {
        Position pos = new Position();
        pos.setCurrentQuantity(qty);
        pos.setTotalCost(BigDecimal.ZERO);
        pos.setAvgCost(BigDecimal.ZERO);
        pos.setRealizedPnl(BigDecimal.ZERO);
        return pos;
    }

    @Test
    void shouldCreateBuyTransaction() {
        User user = mockUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new Portfolio()));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findActiveByPortfolioIdAndStockCode(1L, "600519")).thenReturn(List.of());
        when(calculationService.calculate(1L, "600519", List.of())).thenReturn(mockPosition(new BigDecimal("10")));
        when(positionRepository.findByPortfolioIdAndStockCode(1L, "600519")).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionRecord tx = transactionService.createTransaction("testuser", 1L, "600519", LocalDate.now(),
                TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "买入");

        assertEquals("600519", tx.getStockCode());
        verify(transactionRepository).save(any(TransactionRecord.class));
        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void shouldRejectSellWhenInsufficientPosition() {
        User user = mockUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new Portfolio()));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findActiveByPortfolioIdAndStockCode(1L, "600519")).thenReturn(List.of());
        when(calculationService.calculate(1L, "600519", List.of())).thenReturn(mockPosition(new BigDecimal("-5")));

        assertThrows(InsufficientPositionException.class, () ->
                transactionService.createTransaction("testuser", 1L, "600519", LocalDate.now(),
                        TradeType.SELL, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "卖出"));
    }

    @Test
    void shouldUpdateTransaction() {
        User user = mockUser();
        TransactionRecord existing = new TransactionRecord();
        existing.setId(1L);
        existing.setPortfolioId(1L);
        existing.setStockCode("600519");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new Portfolio()));
        when(transactionRepository.findActiveByPortfolioIdAndStockCode(1L, "600519")).thenReturn(List.of());
        when(calculationService.calculate(1L, "600519", List.of())).thenReturn(mockPosition(new BigDecimal("10")));
        when(positionRepository.findByPortfolioIdAndStockCode(1L, "600519")).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.updateTransaction("testuser", 1L, "600519", LocalDate.now(),
                TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "修改");

        verify(transactionRepository).update(any(TransactionRecord.class));
        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void shouldDeleteTransaction() {
        User user = mockUser();
        TransactionRecord existing = new TransactionRecord();
        existing.setId(1L);
        existing.setPortfolioId(1L);
        existing.setStockCode("600519");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new Portfolio()));
        when(transactionRepository.findActiveByPortfolioIdAndStockCode(1L, "600519")).thenReturn(List.of());
        when(calculationService.calculate(1L, "600519", List.of())).thenReturn(mockPosition(BigDecimal.ZERO));
        when(positionRepository.findByPortfolioIdAndStockCode(1L, "600519")).thenReturn(Optional.empty());

        transactionService.deleteTransaction("testuser", 1L);

        verify(transactionRepository).softDelete(1L);
    }

    @Test
    void shouldSoftDeletePositionWhenLiquidated() {
        User user = mockUser();
        TransactionRecord existing = new TransactionRecord();
        existing.setId(1L);
        existing.setPortfolioId(1L);
        existing.setStockCode("600519");

        Position pos = new Position();
        pos.setId(10L);
        pos.setCurrentQuantity(BigDecimal.ZERO);
        pos.setTotalCost(BigDecimal.ZERO);
        pos.setRealizedPnl(BigDecimal.ZERO);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(portfolioRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new Portfolio()));
        when(transactionRepository.findActiveByPortfolioIdAndStockCode(1L, "600519")).thenReturn(List.of());
        when(calculationService.calculate(1L, "600519", List.of())).thenReturn(pos);
        when(positionRepository.findByPortfolioIdAndStockCode(1L, "600519")).thenReturn(Optional.of(pos));

        transactionService.deleteTransaction("testuser", 1L);

        verify(positionRepository).softDelete(10L);
    }

    @Test
    void shouldThrowWhenTransactionNotFound() {
        User user = mockUser();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.updateTransaction("testuser", 1L, "600519", LocalDate.now(),
                        TradeType.BUY, new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, ""));
    }
}
