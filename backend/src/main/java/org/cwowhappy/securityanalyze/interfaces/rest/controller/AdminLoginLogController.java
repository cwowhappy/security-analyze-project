package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.interfaces.rest.support.AuthContextHelper;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.user.application.dto.LoginLogDTO;
import org.cwowhappy.securityanalyze.user.application.service.AdminLogAppService;
import org.cwowhappy.securityanalyze.user.domain.model.LoginLog;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员登录日志 REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/login-logs")
@RequiredArgsConstructor
public class AdminLoginLogController {

    private final AdminLogAppService adminLogAppService;
    private final AuthContextHelper authContextHelper;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<LoginLogDTO>>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletRequest request) {

        assertAdmin(request);
        PageResult<LoginLog> result = adminLogAppService.queryLogs(userId, action, startDate, endDate, page, size);
        PageResult<LoginLogDTO> dtoResult = PageResult.<LoginLogDTO>builder()
                .list(result.getList().stream().map(this::toDTO).toList())
                .total(result.getTotal())
                .page(result.getPage())
                .size(result.getSize())
                .build();
        return ResponseEntity.ok(ApiResponse.success(dtoResult));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletRequest request) {

        assertAdmin(request);
        List<LoginLog> logs = adminLogAppService.exportLogs(userId, action, startDate, endDate);
        String csv = toCsv(logs);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"login-logs.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    private void assertAdmin(HttpServletRequest request) {
        var user = authContextHelper.getCurrentUser(request);
        if (!"admin".equals(user.getRole())) {
            throw new org.cwowhappy.securityanalyze.shared.exception.ApplicationException("权限不足，仅管理员可访问");
        }
    }

    private LoginLogDTO toDTO(LoginLog log) {
        return LoginLogDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .action(log.getAction())
                .ip(log.getIp())
                .userAgent(log.getUserAgent())
                .details(log.getDetails())
                .timestamp(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null)
                .build();
    }

    private String toCsv(List<LoginLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // BOM for Excel
        sb.append("ID,用户ID,用户名,操作,IP,设备,详情,时间\n");
        for (LoginLog log : logs) {
            sb.append(log.getId()).append(",")
                    .append(escapeCsv(log.getUserId())).append(",")
                    .append(escapeCsv(log.getUsername())).append(",")
                    .append(escapeCsv(log.getAction())).append(",")
                    .append(escapeCsv(log.getIp())).append(",")
                    .append(escapeCsv(log.getUserAgent())).append(",")
                    .append(escapeCsv(log.getDetails())).append(",")
                    .append(escapeCsv(log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""))
                    .append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }
}
