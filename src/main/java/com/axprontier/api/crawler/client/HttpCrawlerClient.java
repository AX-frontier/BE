package com.axprontier.api.crawler.client;

import com.axprontier.api.crawler.dto.CrawlerTriggerRequest;
import com.axprontier.api.crawler.dto.CrawlerRunResponse;
import com.axprontier.api.crawler.dto.CrawlerStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpCrawlerClient implements CrawlerClient {

    private final RestClient restClient;
    private final String triggerPath;
    private final String statusPath;

    public HttpCrawlerClient(
            @Value("${ai.server.base-url}") String aiServerBaseUrl,
            @Value("${crawler.api.trigger-path}") String triggerPath,
            @Value("${crawler.api.status-path}") String statusPath
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServerBaseUrl)
                .build();
        this.triggerPath = triggerPath;
        this.statusPath = statusPath;
    }

    @Override
    public CrawlerRunResponse trigger(CrawlerTriggerRequest request) {
        return restClient.post()
                .uri(triggerPath)
                .body(request)
                .retrieve()
                .body(CrawlerRunResponse.class);
    }

    @Override
    public CrawlerStatusResponse getStatus(String jobId) {
        return restClient.get()
                .uri(statusPath, jobId)
                .retrieve()
                .body(CrawlerStatusResponse.class);
    }
}
