package com.axprontier.api.crawler.dto;

import java.util.Map;

public record CrawlerStatusResponse(
        String jobId,
        String status,
        Map<String, Object> result,
        String error
) {
}
