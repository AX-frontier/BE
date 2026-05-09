package com.axprontier.api.crawler.client;

import com.axprontier.api.crawler.dto.CrawlerTriggerRequest;
import com.axprontier.api.crawler.dto.CrawlerRunResponse;
import com.axprontier.api.crawler.dto.CrawlerStatusResponse;

public interface CrawlerClient {

    CrawlerRunResponse trigger(CrawlerTriggerRequest request);

    CrawlerStatusResponse getStatus(String jobId);
}
