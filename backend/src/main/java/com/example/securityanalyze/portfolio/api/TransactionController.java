package com.example.securityanalyze.portfolio.api;

import com.example.securityanalyze.common.util.PageUtils;
import com.example.securityanalyze.portfolio.application.TransactionService;
import com.example.securityanalyze.portfolio.domain.TradeType;
import com.example.securityanalyze.portfolio.domain.TransactionRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    private static final Map<TradeType, String> TRADE_TYPE_LABELS = Map.of(
            TradeType.BUY, "买入",
            TradeType.SELL, "卖出",
            TradeType.DIVIDEND, "现金分红",
            TradeType.BONUS, "送股",
            TradeType.RIGHTS, "配股",
            TradeType.SPLIT, "股份拆分",
            TradeType.MERGER, "吸收合并",
            TradeType.OTHER, "其他"
    );

    @GetMapping("/portfolios/{portfolioId}/transactions")
    public ResponseEntity<TransactionListResponse> listTransactions(
            @PathVariable Long portfolioId,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) TradeType tradeType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int[] normalized = PageUtils.normalize(page, size);
        List<TransactionRecord> items = transactionService.listTransactions(
                portfolioId, stockCode, tradeType, startDate, endDate, normalized[0], normalized[1]);
        long total = transactionService.countTransactions(portfolioId, stockCode, tradeType, startDate, endDate);

        TransactionListResponse response = new TransactionListResponse();
        response.setItems(items.stream().map(this::toResponse).toList());
        response.setTotal(total);
        response.setPage(normalized[0]);
        response.setSize(normalized[1]);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/portfolios/{portfolioId}/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId,
            @Valid @RequestBody TransactionRequest request) {
        TransactionRecord tx = transactionService.createTransaction(
                userDetails.getUsername(), portfolioId, request.getStockCode(), request.getTradeDate(),
                request.getTradeType(), request.getPrice(), request.getQuantity(),
                request.getFee(), request.getTax(), request.getRemark());
        return ResponseEntity.ok(toResponse(tx));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        TransactionRecord tx = transactionService.updateTransaction(
                userDetails.getUsername(), id, request.getStockCode(), request.getTradeDate(),
                request.getTradeType(), request.getPrice(), request.getQuantity(),
                request.getFee(), request.getTax(), request.getRemark());
        return ResponseEntity.ok(toResponse(tx));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        transactionService.deleteTransaction(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/portfolios/{portfolioId}/transactions/import")
    public ResponseEntity<ImportResultResponse> importTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId,
            @RequestParam("file") MultipartFile file) {
        ImportResultResponse result = new ImportResultResponse();
        List<ImportError> errors = new ArrayList<>();
        int success = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String header = reader.readLine();
            if (header == null) {
                result.setTotal(0);
                result.setSuccess(0);
                result.setErrors(errors);
                return ResponseEntity.ok(result);
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    TransactionRequest req = parseCsvLine(line);
                    transactionService.createTransaction(
                            userDetails.getUsername(), portfolioId, req.getStockCode(), req.getTradeDate(),
                            req.getTradeType(), req.getPrice(), req.getQuantity(),
                            req.getFee(), req.getTax(), req.getRemark());
                    success++;
                } catch (Exception e) {
                    errors.add(new ImportError(lineNum, line, e.getMessage()));
                    log.warn("导入第{}行失败: {}", lineNum, e.getMessage());
                }
            }

            result.setTotal(lineNum - 1);
            result.setSuccess(success);
            result.setErrors(errors);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("CSV导入失败", e);
            result.setTotal(0);
            result.setSuccess(0);
            errors.add(new ImportError(0, "", e.getMessage()));
            result.setErrors(errors);
            return ResponseEntity.badRequest().body(result);
        }
    }

    private TransactionRequest parseCsvLine(String line) {
        String[] cols = line.split(",");
        if (cols.length < 5) {
            throw new IllegalArgumentException("列数不足，至少需要5列: 股票代码,交易日期,交易类型,价格,数量");
        }
        TransactionRequest req = new TransactionRequest();
        req.setStockCode(cols[0].trim());
        req.setTradeDate(LocalDate.parse(cols[1].trim()));
        req.setTradeType(TradeType.valueOf(cols[2].trim().toUpperCase()));
        String priceStr = cols[3].trim();
        req.setPrice(priceStr.isEmpty() ? null : new BigDecimal(priceStr));
        req.setQuantity(new BigDecimal(cols[4].trim()));
        req.setFee(cols.length > 5 && !cols[5].trim().isEmpty() ? new BigDecimal(cols[5].trim()) : BigDecimal.ZERO);
        req.setTax(cols.length > 6 && !cols[6].trim().isEmpty() ? new BigDecimal(cols[6].trim()) : BigDecimal.ZERO);
        req.setRemark(cols.length > 7 ? cols[7].trim() : null);
        return req;
    }

    @lombok.Data
    public static class ImportResultResponse {
        private int total;
        private int success;
        private List<ImportError> errors;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ImportError {
        private int line;
        private String content;
        private String message;
    }

    private TransactionResponse toResponse(TransactionRecord tx) {
        TransactionResponse response = new TransactionResponse();
        response.setId(tx.getId());
        response.setPortfolioId(tx.getPortfolioId());
        response.setStockCode(tx.getStockCode());
        response.setTradeDate(tx.getTradeDate());
        response.setTradeType(tx.getTradeType());
        response.setTradeTypeLabel(TRADE_TYPE_LABELS.getOrDefault(tx.getTradeType(), tx.getTradeType().name()));
        response.setPrice(tx.getPrice());
        response.setQuantity(tx.getQuantity());
        response.setFee(tx.getFee());
        response.setTax(tx.getTax());
        response.setAmount(tx.getAmount());
        response.setRealizedPnl(tx.getRealizedPnl());
        response.setRemark(tx.getRemark());
        response.setCreatedAt(tx.getCreatedAt());
        return response;
    }

    @lombok.Data
    public static class TransactionListResponse {
        private List<TransactionResponse> items;
        private long total;
        private int page;
        private int size;
    }
}
