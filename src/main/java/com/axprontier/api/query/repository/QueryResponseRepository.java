package com.axprontier.api.query.repository;

import com.axprontier.api.query.entity.QueryResponse;
import com.axprontier.api.query.entity.Query;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryResponseRepository extends JpaRepository<QueryResponse, Long> {

    Optional<QueryResponse> findByQuery(Query query);

    List<QueryResponse> findByQueryIn(List<Query> queries);
}
