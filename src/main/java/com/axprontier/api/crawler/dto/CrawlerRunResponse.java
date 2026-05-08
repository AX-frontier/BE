package com.axprontier.api.crawler.dto;

public record CrawlerRunResponse(
        String jobId,
        String status,
        String statusUrl
) {
}
