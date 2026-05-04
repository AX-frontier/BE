package com.axprontier.api.ai.repository;

import com.axprontier.api.ai.entity.AiResponseLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiResponseLogRepository extends JpaRepository<AiResponseLog, Long> {
}
