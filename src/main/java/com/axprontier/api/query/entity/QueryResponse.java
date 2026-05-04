package com.axprontier.api.query.entity;

import com.axprontier.api.global.entity.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "core", name = "responses")
public class QueryResponse extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "answer_text", nullable = false, columnDefinition = "text")
    private String answerText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sources_json", columnDefinition = "jsonb")
    private Map<String, Object> sourcesJson;

    @Column(name = "source_count", nullable = false)
    private int sourceCount;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "fallback_reason", columnDefinition = "text")
    private String fallbackReason;

    protected QueryResponse() {
    }

    public QueryResponse(Query query, String answerText, Map<String, Object> sourcesJson, int sourceCount, BigDecimal confidence, String fallbackReason) {
        this.query = query;
        this.answerText = answerText;
        this.sourcesJson = sourcesJson;
        this.sourceCount = sourceCount;
        this.confidence = confidence;
        this.fallbackReason = fallbackReason;
    }
}
