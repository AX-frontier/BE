package com.axprontier.api.query.entity;

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
import java.math.BigDecimal;

@Entity
@Table(schema = "core", name = "routes")
public class QueryRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "predicted_intent", length = 100)
    private String predictedIntent;

    @Column(name = "target_agent", nullable = false, length = 50)
    private String targetAgent;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    protected QueryRoute() {
    }

    public QueryRoute(Query query, String predictedIntent, String targetAgent, BigDecimal confidence, boolean fallbackUsed) {
        this.query = query;
        this.predictedIntent = predictedIntent;
        this.targetAgent = targetAgent;
        this.confidence = confidence;
        this.fallbackUsed = fallbackUsed;
    }
}
