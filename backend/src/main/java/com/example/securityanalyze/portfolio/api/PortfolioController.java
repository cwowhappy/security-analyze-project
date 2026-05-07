package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.portfolio.application.PortfolioService;
import com.example.securityanalyze.portfolio.domain.Portfolio;
import com.example.securityanalyze.portfolio.domain.PortfolioType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> listPortfolios(@AuthenticationPrincipal UserDetails userDetails) {
        List<Portfolio> portfolios = portfolioService.listPortfolios(userDetails.getUsername());
        List<PortfolioResponse> responses = portfolios.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PortfolioRequest request) {
        Portfolio portfolio = portfolioService.createPortfolio(
                userDetails.getUsername(), request.getName(), request.getType(), request.getBroker(), request.getDescription());
        return ResponseEntity.ok(toResponse(portfolio));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody PortfolioRequest request) {
        portfolioService.updatePortfolio(userDetails.getUsername(), id, request.getName(), request.getType(),
                request.getBroker(), request.getDescription());
        Portfolio portfolio = portfolioService.getPortfolio(userDetails.getUsername(), id);
        return ResponseEntity.ok(toResponse(portfolio));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        portfolioService.deletePortfolio(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        PortfolioResponse response = new PortfolioResponse();
        response.setId(portfolio.getId());
        response.setName(portfolio.getName());
        response.setType(portfolio.getType());
        response.setBroker(portfolio.getBroker());
        response.setDescription(portfolio.getDescription());
        response.setCreatedAt(portfolio.getCreatedAt());
        return response;
    }
}
