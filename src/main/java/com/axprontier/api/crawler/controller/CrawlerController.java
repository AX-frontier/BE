package com.axprontier.api.crawler.controller;

import com.axprontier.api.crawler.dto.CrawlerStatusResponse;
import com.axprontier.api.crawler.dto.CrawlerTriggerRequest;
import com.axprontier.api.crawler.dto.CrawlerTriggerResponse;
import com.axprontier.api.crawler.service.CrawlerService;
import com.axprontier.api.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawlers")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/trigger")
    public ApiResponse<CrawlerTriggerResponse> trigger(@Valid @RequestBody CrawlerTriggerRequest request) {
        return ApiResponse.ok(crawlerService.trigger(request));
    }

    @GetMapping("/status/{jobId}")
    public ApiResponse<CrawlerStatusResponse> getStatus(@PathVariable String jobId) {
        return ApiResponse.ok(crawlerService.getStatus(jobId));
    }
}
