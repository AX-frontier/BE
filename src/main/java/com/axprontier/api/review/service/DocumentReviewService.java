package com.axprontier.api.review.service;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryResponse;
import com.axprontier.api.review.dto.DocumentReviewRequest;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentReviewService {

    private static final Logger log = LoggerFactory.getLogger(DocumentReviewService.class);

    private final AiGatewayService aiGatewayService;

    public DocumentReviewService(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public CoreQueryResponse review(DocumentReviewRequest request) {
        Instant startedAt = Instant.now();
        OrchestrateRequest aiRequest = new OrchestrateRequest(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                request.message(),
                request.document()
        );

        try {
            OrchestrateResponse response = aiGatewayService.chat(TargetAgent.DOCUMENT_REVIEW, aiRequest);
            logResult(request, response, "COMPLETED", startedAt);
            return toCoreResponse(response);
        } catch (RuntimeException exception) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse(
                    TargetAgent.DOCUMENT_REVIEW.name(),
                    "DOCUMENT_REVIEW_AGENT_CALL_FAILED"
            );
            logResult(request, fallbackResponse, "FAILED", startedAt);
            return toCoreResponse(fallbackResponse);
        }
    }

    private void logResult(
            DocumentReviewRequest request,
            OrchestrateResponse response,
            String status,
            Instant startedAt
    ) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
                "document_review queryUid={} traceId={} conversationUid={} status={} latencyMs={} fallbackUsed={}",
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                status,
                latencyMs,
                response.fallbackUsed()
        );
    }

    private CoreQueryResponse toCoreResponse(OrchestrateResponse response) {
        return new CoreQueryResponse(
                response.targetAgent(),
                response.intent(),
                response.answer(),
                response.sources(),
                response.confidence(),
                response.fallbackUsed(),
                response.fallbackReason(),
                response.searchKeyword(),
                response.resultCount(),
                response.matchedBooks(),
                response.summary(),
                response.findings(),
                response.criterionResults(),
                response.checkRequiredItems(),
                response.formatNoticeItems(),
                response.extractedTables(),
                response.revisedDocument(),
                response.reviewMarkdown()
        );
    }
}
