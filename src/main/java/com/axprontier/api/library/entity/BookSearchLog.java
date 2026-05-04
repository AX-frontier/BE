package com.axprontier.api.library.entity;

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
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "library", name = "book_search_logs")
public class BookSearchLog extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "keyword", length = 300)
    private String keyword;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "results_json", columnDefinition = "jsonb")
    private Map<String, Object> resultsJson;

    protected BookSearchLog() {
    }

    public BookSearchLog(Query query, String keyword, int resultCount, Map<String, Object> resultsJson) {
        this.query = query;
        this.keyword = keyword;
        this.resultCount = resultCount;
        this.resultsJson = resultsJson;
    }
}
