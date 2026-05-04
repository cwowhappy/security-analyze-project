package com.example.securityanalyze.industry.infrastructure;

import com.example.securityanalyze.industry.api.TrendDataPoint;
import com.example.securityanalyze.industry.domain.IndustryTrendGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndustryTrendAdapter implements IndustryTrendGateway {

    private final ObjectMapper objectMapper;

    @Override
    public List<TrendDataPoint> fetchTrend(String industryName, String period) {
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
                log.warn("行业趋势脚本执行超时, industry={}, period={}", industryName, period);
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
            log.warn("行业趋势脚本执行失败, industry={}, period={}, error={}", industryName, period, e.getMessage());
            return List.of();
        }
    }
}
