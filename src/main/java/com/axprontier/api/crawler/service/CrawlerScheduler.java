package com.axprontier.api.crawler.service;

import com.axprontier.api.crawler.dto.CrawlerTriggerRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlerScheduler {

    private final CrawlerService crawlerService;
    private final List<String> defaultSources;
    private final int defaultSinceMonths;
    private final int defaultMaxPages;
    private final int defaultMaxNoticePages;
    private final boolean pollingEnabled;

    public CrawlerScheduler(
            CrawlerService crawlerService,
            @Value("${crawler.schedule.default-sources:hansung_notice,hsel_library}") List<String> defaultSources,
            @Value("${crawler.schedule.default-since-months:6}") int defaultSinceMonths,
            @Value("${crawler.schedule.default-max-pages:5}") int defaultMaxPages,
            @Value("${crawler.schedule.default-max-notice-pages:30}") int defaultMaxNoticePages,
            @Value("${crawler.polling.enabled:true}") boolean pollingEnabled
    ) {
        this.crawlerService = crawlerService;
        this.defaultSources = defaultSources;
        this.defaultSinceMonths = defaultSinceMonths;
        this.defaultMaxPages = defaultMaxPages;
        this.defaultMaxNoticePages = defaultMaxNoticePages;
        this.pollingEnabled = pollingEnabled;
    }

    @Scheduled(cron = "${crawler.schedule.cron:-}")
    public void triggerScheduledCrawl() {
        crawlerService.trigger(new CrawlerTriggerRequest(
                defaultSources,
                LocalDate.now().minusMonths(defaultSinceMonths),
                defaultMaxPages,
                defaultMaxNoticePages,
                false
        ));
    }

    @Scheduled(fixedDelayString = "${crawler.polling.fixed-delay-ms:60000}")
    public void pollCrawlerStatus() {
        if (pollingEnabled) {
            crawlerService.pollRunningJobs();
        }
    }
}
