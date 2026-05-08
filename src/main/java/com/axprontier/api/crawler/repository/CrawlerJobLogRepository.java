package com.axprontier.api.crawler.repository;

import com.axprontier.api.crawler.entity.CrawlerJobLog;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlerJobLogRepository extends JpaRepository<CrawlerJobLog, Long> {

    Optional<CrawlerJobLog> findByRequestUid(UUID requestUid);

    Optional<CrawlerJobLog> findByExternalJobId(String externalJobId);

    List<CrawlerJobLog> findByStatusIn(Collection<String> statuses);
}
