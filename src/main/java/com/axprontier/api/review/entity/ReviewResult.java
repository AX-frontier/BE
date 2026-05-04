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
import java.math.BigDecimal;

@Entity
@Table(schema = "doc", name = "review_results")
public class ReviewResult extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ReviewRequest request;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    protected ReviewResult() {
    }

    public ReviewResult(ReviewRequest request, BigDecimal score, String summary) {
        this.request = request;
        this.score = score;
        this.summary = summary;
    }
}
