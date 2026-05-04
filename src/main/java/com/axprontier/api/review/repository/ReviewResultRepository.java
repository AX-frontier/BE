package com.axprontier.api.review.repository;

import com.axprontier.api.review.entity.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {
}
