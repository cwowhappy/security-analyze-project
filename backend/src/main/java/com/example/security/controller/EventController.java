package com.example.security.controller;

import com.example.security.domain.entity.SecurityEvent;
import com.example.security.dto.ApiResponse;
import com.example.security.repository.SecurityEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final SecurityEventRepository securityEventRepository;

    public EventController(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    @GetMapping
    public ApiResponse<Page<SecurityEvent>> list(Pageable pageable) {
        return ApiResponse.ok(securityEventRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @PostMapping
    public ApiResponse<SecurityEvent> create(@RequestBody SecurityEvent event) {
        return ApiResponse.ok(securityEventRepository.save(event));
    }
}
