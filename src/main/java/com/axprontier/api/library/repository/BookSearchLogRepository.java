package com.axprontier.api.library.repository;

import com.axprontier.api.library.entity.BookSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookSearchLogRepository extends JpaRepository<BookSearchLog, Long> {
}
