package com.axprontier.api.query.repository;

import com.axprontier.api.query.entity.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
}
