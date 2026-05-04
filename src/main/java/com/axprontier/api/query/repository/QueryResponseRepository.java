package com.axprontier.api.query.repository;

import com.axprontier.api.query.entity.QueryResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryResponseRepository extends JpaRepository<QueryResponse, Long> {
}
