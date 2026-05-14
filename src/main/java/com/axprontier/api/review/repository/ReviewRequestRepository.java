package com.axprontier.api.review.repository;

import com.axprontier.api.review.entity.ReviewRequest;
import com.axprontier.api.query.entity.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {

    boolean existsByQuery(Query query);
}
