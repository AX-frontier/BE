package com.axprontier.api.crawler.service;

import com.axprontier.api.crawler.client.CrawlerClient;
import com.axprontier.api.crawler.dto.CrawlerRunResponse;
import com.axprontier.api.crawler.dto.CrawlerStatusResponse;
import com.axprontier.api.crawler.dto.CrawlerTriggerRequest;
import com.axprontier.api.crawler.dto.CrawlerTriggerResponse;
import com.axprontier.api.crawler.entity.CrawlerJobLog;
import com.axprontier.api.crawler.exception.CrawlerErrorCode;
import com.axprontier.api.crawler.repository.CrawlerJobLogRepository;
import com.axprontier.api.global.apiPayload.code.GeneralErrorCode;
import com.axprontier.api.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CrawlerService {

    private final CrawlerClient crawlerClient;
    private final CrawlerJobLogRepository crawlerJobLogRepository;
    private final ObjectMapper objectMapper;

    public CrawlerService(
            CrawlerClient crawlerClient,
            CrawlerJobLogRepository crawlerJobLogRepository,
            ObjectMapper objectMapper
    ) {
        this.crawlerClient = crawlerClient;
        this.crawlerJobLogRepository = crawlerJobLogRepository;
        this.objectMapper = objectMapper;
    }

    public CrawlerTriggerResponse trigger(CrawlerTriggerRequest request) {
        CrawlerJobLog jobLog = crawlerJobLogRepository.save(new CrawlerJobLog(
                UUID.randomUUID(),
                toMap(request)
        ));

        try {
            CrawlerRunResponse result = crawlerClient.trigger(request);
            jobLog.markAccepted(result.jobId(), result.status(), result.statusUrl(), toMap(result));
            CrawlerJobLog savedLog = crawlerJobLogRepository.save(jobLog);
            return toResponse(savedLog);
        } catch (Exception exception) {
            jobLog.markFailed(exception.getMessage());
            crawlerJobLogRepository.save(jobLog);
            throw new GeneralException(CrawlerErrorCode.CRAWLER_API_FAILED, exception.getMessage());
        }
    }

    public CrawlerStatusResponse getStatus(String externalJobId) {
        CrawlerJobLog jobLog = crawlerJobLogRepository.findByExternalJobId(externalJobId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        return refreshStatus(jobLog);
    }

    public void pollRunningJobs() {
        List<CrawlerJobLog> jobLogs = crawlerJobLogRepository.findByStatusIn(List.of("ACCEPTED", "RUNNING", "REQUESTED"));
        for (CrawlerJobLog jobLog : jobLogs) {
            if (jobLog.getExternalJobId() != null) {
                refreshStatus(jobLog);
            }
        }
    }

    private CrawlerStatusResponse refreshStatus(CrawlerJobLog jobLog) {
        try {
            CrawlerStatusResponse status = crawlerClient.getStatus(jobLog.getExternalJobId());
            jobLog.updateStatus(status.status(), toMap(status));
            crawlerJobLogRepository.save(jobLog);
            return status;
        } catch (Exception exception) {
            jobLog.markFailed(exception.getMessage());
            crawlerJobLogRepository.save(jobLog);
            throw new GeneralException(CrawlerErrorCode.CRAWLER_API_FAILED, exception.getMessage());
        }
    }

    private CrawlerTriggerResponse toResponse(CrawlerJobLog jobLog) {
        return new CrawlerTriggerResponse(
                jobLog.getRequestUid(),
                jobLog.getExternalJobId(),
                jobLog.getStatus(),
                jobLog.getStatusUrl(),
                jobLog.getStartedAt(),
                jobLog.getEndedAt()
        );
    }

    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }
}
