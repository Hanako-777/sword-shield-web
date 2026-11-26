package com.swordshield.ai.repository;

import com.swordshield.ai.domain.AiChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatLogRepository extends JpaRepository<AiChatLog, Long> {
    Page<AiChatLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<AiChatLog> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
}
