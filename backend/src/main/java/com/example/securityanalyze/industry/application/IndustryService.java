package com.example.securityanalyze.industry.application;

import com.example.securityanalyze.company.api.CompanyListItem;
import com.example.securityanalyze.company.api.CompanyListResponse;
import com.example.securityanalyze.industry.api.IndustryListItem;
import com.example.securityanalyze.industry.api.IndustryListResponse;
import com.example.securityanalyze.industry.api.IndustryTrendResponse;
import com.example.securityanalyze.industry.api.TrendDataPoint;
import com.example.securityanalyze.industry.infrastructure.IndustryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryService {

    private final IndustryRepository industryRepository;
    private final ObjectMapper objectMapper;

    public IndustryListResponse listIndustries() {
        List<IndustryListItem> items = industryRepository.findIndustries();
        IndustryListResponse response = new IndustryListResponse();
        response.setData(items);
        response.setTotal(items.size());
        return response;
    }

    public CompanyListResponse listCompaniesByIndustry(String industryName, int page, int size) {
        int offset = page * size;

        List<CompanyListItem> items = industryRepository.findCompaniesByIndustry(industryName, offset, size);
        long total = industryRepository.countCompaniesByIndustry(industryName);

        CompanyListResponse response = new CompanyListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    public IndustryTrendResponse getIndustryTrend(String industryName, String period) {
        IndustryTrendResponse response = new IndustryTrendResponse();
        response.setIndustryName(industryName);
        response.setPeriod(period);

        // 尝试调用 akshare 获取真实数据
        List<TrendDataPoint> realData = fetchTrendFromAkshare(industryName, period);
        if (!realData.isEmpty()) {
            response.setData(realData);
            response.setFallback(false);
            return response;
        }

        // 失败则返回模拟数据用于 UI 展示
        log.warn("Failed to fetch real trend data for {}, returning fallback data", industryName);
        response.setData(generateFallbackTrend(period));
        response.setFallback(true);
        return response;
    }

    private List<TrendDataPoint> fetchTrendFromAkshare(String industryName, String period) {
        try {
            String projectRoot = System.getProperty("user.dir");
            ProcessBuilder pb = new ProcessBuilder(
                    "python3",
                    projectRoot + "/collector/scripts/industry_trend.py",
                    industryName,
                    period
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Python script timed out for industry trend: {}", industryName);
                return List.of();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                String json = output.toString().trim();
                if (json.isBlank() || !json.startsWith("[") && !json.startsWith("{")) {
                    return List.of();
                }
                return objectMapper.readValue(json, new TypeReference<List<TrendDataPoint>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to execute python script for industry trend: {}", e.getMessage());
            return List.of();
        }
    }

    private List<TrendDataPoint> generateFallbackTrend(String period) {
        int days = switch (period) {
            case "1m" -> 22;
            case "6m" -> 132;
            case "1y" -> 250;
            default -> 66; // 3m
        };

        List<TrendDataPoint> data = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(days);
        double baseValue = 1000.0;

        for (int i = 0; i < days; i++) {
            TrendDataPoint point = new TrendDataPoint();
            point.setDate(date.format(DateTimeFormatter.ISO_DATE));

            double change = (Math.random() - 0.48) * 2.0; // 轻微上涨趋势
            baseValue = baseValue * (1 + change / 100);
            point.setClose(BigDecimal.valueOf(baseValue).setScale(2, RoundingMode.HALF_UP));
            point.setChangePercent(BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP));

            data.add(point);
            date = date.plusDays(1);
        }
        return data;
    }
}
