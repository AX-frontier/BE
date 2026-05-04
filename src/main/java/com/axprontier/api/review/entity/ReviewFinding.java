package com.axprontier.api.review.entity;

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

@Entity
@Table(schema = "doc", name = "review_findings")
public class ReviewFinding extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private ReviewResult result;

    @Column(name = "section_type", length = 20)
    private String sectionType;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "suggestion", columnDefinition = "text")
    private String suggestion;

    @Column(name = "line_start")
    private Integer lineStart;

    @Column(name = "line_end")
    private Integer lineEnd;

    protected ReviewFinding() {
    }

    public ReviewFinding(ReviewResult result, String sectionType, String severity, String message, String suggestion, Integer lineStart, Integer lineEnd) {
        this.result = result;
        this.sectionType = sectionType;
        this.severity = severity;
        this.message = message;
        this.suggestion = suggestion;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
    }
}
