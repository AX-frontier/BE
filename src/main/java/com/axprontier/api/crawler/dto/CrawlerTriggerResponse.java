package com.axprontier.api.crawler.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrawlerTriggerResponse(
        UUID requestUid,
        String externalJobId,
        String status,
        String statusUrl,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
