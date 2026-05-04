package com.axprontier.api.ai.entity;

import com.axprontier.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "core", name = "ai_responses")
public class AiResponseLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_request_id", nullable = false)
    private AiRequestLog aiRequest;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", columnDefinition = "jsonb")
    private Map<String, Object> responseJson;

    @Column(name = "latency_ms")
    private Long latencyMs;

    protected AiResponseLog() {
    }

    public AiResponseLog(AiRequestLog aiRequest, int statusCode, Map<String, Object> responseJson, Long latencyMs) {
        this.aiRequest = aiRequest;
        this.statusCode = statusCode;
        this.responseJson = responseJson;
        this.latencyMs = latencyMs;
    }
}
