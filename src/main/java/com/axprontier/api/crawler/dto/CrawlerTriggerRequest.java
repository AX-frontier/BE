package com.axprontier.api.crawler.dto;

import java.time.LocalDate;
import java.util.List;

public record CrawlerTriggerRequest(
        List<String> sources,
        LocalDate sinceDate,
        Integer maxPages,
        Integer maxNoticePages,
        Boolean initSchema
) {
}
