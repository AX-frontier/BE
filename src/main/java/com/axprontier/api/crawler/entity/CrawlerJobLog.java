package com.axprontier.api.crawler.entity;

import com.axprontier.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "ingestion", name = "crawler_job_logs")
public class CrawlerJobLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_uid", nullable = false, unique = true)
    private UUID requestUid;

    @Column(name = "external_job_id", length = 100)
    private String externalJobId;

    @Column(name = "status_url", length = 500)
    private String statusUrl;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_json", columnDefinition = "jsonb")
    private Map<String, Object> requestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", columnDefinition = "jsonb")
    private Map<String, Object> responseJson;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected CrawlerJobLog() {
    }

    public CrawlerJobLog(UUID requestUid, Map<String, Object> requestJson) {
        this.requestUid = requestUid;
        this.status = "REQUESTED";
        this.requestJson = requestJson;
        this.startedAt = LocalDateTime.now();
    }

    public void markAccepted(String externalJobId, String status, String statusUrl, Map<String, Object> responseJson) {
        this.externalJobId = externalJobId;
        this.status = status;
        this.statusUrl = statusUrl;
        this.responseJson = responseJson;
    }

    public void updateStatus(String status, Map<String, Object> responseJson) {
        this.status = status;
        this.responseJson = responseJson;
        if (isTerminal(status)) {
            this.endedAt = LocalDateTime.now();
        }
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.endedAt = LocalDateTime.now();
    }

    public UUID getRequestUid() {
        return requestUid;
    }

    public String getExternalJobId() {
        return externalJobId;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Object> getResponseJson() {
        return responseJson;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    private boolean isTerminal(String status) {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status) || "COMPLETED".equals(status);
    }
}
