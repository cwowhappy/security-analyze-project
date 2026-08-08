package com.example.security.repository;

import com.example.security.domain.entity.SecurityEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SecurityEventRepositoryTest {

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Test
    void shouldSaveAndRetrieveEventsOrderedByCreatedAtDesc() {
        SecurityEvent event = new SecurityEvent();
        event.setTitle("SQL 注入尝试");
        event.setSeverity("HIGH");
        event.setSource("WAF");
        event.setEventType("ATTACK");
        securityEventRepository.save(event);

        Page<SecurityEvent> events = securityEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(events.getContent()).hasSize(1);
        assertThat(events.getContent().get(0).getTitle()).isEqualTo("SQL 注入尝试");
    }
}
