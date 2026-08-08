package com.example.security.repository;

import com.example.security.domain.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    Page<SecurityEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
