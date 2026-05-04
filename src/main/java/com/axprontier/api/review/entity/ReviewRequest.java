package com.axprontier.api.review.entity;

import com.axprontier.api.global.entity.CreatedAtEntity;
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

@Entity
@Table(schema = "doc", name = "review_requests")
public class ReviewRequest extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(name = "body_text", nullable = false, columnDefinition = "text")
    private String bodyText;

    protected ReviewRequest() {
    }

    public ReviewRequest(Query query, String title, String docType, String bodyText) {
        this.query = query;
        this.title = title;
        this.docType = docType;
        this.bodyText = bodyText;
    }
}
