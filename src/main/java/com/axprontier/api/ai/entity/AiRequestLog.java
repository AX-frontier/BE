package com.axprontier.api.ai.entity;

import com.axprontier.api.global.entity.BaseEntity;
import com.axprontier.api.query.entity.Query;
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
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "core", name = "ai_requests")
public class AiRequestLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "trace_id", nullable = false)
    private UUID traceId;

    @Column(name = "endpoint", nullable = false, length = 200)
    private String endpoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestJson;

    protected AiRequestLog() {
    }

    public AiRequestLog(Query query, UUID traceId, String endpoint, Map<String, Object> requestJson) {
        this.query = query;
        this.traceId = traceId;
        this.endpoint = endpoint;
        this.requestJson = requestJson;
    }
}
