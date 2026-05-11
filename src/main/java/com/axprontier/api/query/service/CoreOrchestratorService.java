package com.axprontier.api.query.service;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.dto.CoreQueryResponse;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class CoreOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(CoreOrchestratorService.class);

    private final AiGatewayService aiGatewayService;

    public CoreOrchestratorService(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public CoreQueryResponse query(CoreQueryRequest request) {
        Instant startedAt = Instant.now();
        OrchestrateRequest orchestrateRequest = new OrchestrateRequest(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                request.message(),
                request.document()
        );

        try {
            OrchestrateResponse response = aiGatewayService.orchestrateChat(orchestrateRequest);
            log.info(
                    "core_orchestrator_chat traceId={} targetAgent={} intent={} confidence={} fallbackUsed={} fallbackReason={} statusCode={} timeout=false",
                    request.traceId(),
                    response.targetAgent(),
                    response.intent(),
                    response.confidence(),
                    response.fallbackUsed(),
                    response.fallbackReason(),
                    200
            );
            logResult(request, response, "COMPLETED", startedAt, 200, false);
            return toCoreResponse(response);
        } catch (RuntimeException exception) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse("FALLBACK", "ORCHESTRATOR_CHAT_FAILED");
            log.warn(
                    "core_orchestrator_chat traceId={} targetAgent={} statusCode={} timeout={} error={}",
                    request.traceId(),
                    TargetAgent.FALLBACK.name(),
                    statusCode(exception),
                    isTimeout(exception),
                    exception.getClass().getSimpleName()
            );
            logResult(request, fallbackResponse, "FAILED", startedAt, statusCode(exception), isTimeout(exception));
            return toCoreResponse(fallbackResponse);
        }
    }

    private void logResult(
            CoreQueryRequest request,
            OrchestrateResponse response,
            String status,
            Instant startedAt,
            int statusCode,
            boolean timeout
    ) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
                "core_orchestrator queryUid={} traceId={} conversationUid={} message={} targetAgent={} intent={} confidence={} status={} latencyMs={} fallbackUsed={} fallbackReason={} statusCode={} timeout={}",
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                request.message(),
                response.targetAgent(),
                response.intent(),
                response.confidence(),
                status,
                latencyMs,
                response.fallbackUsed(),
                response.fallbackReason(),
                statusCode,
                timeout
        );
    }

    private int statusCode(RuntimeException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value();
        }
        return 500;
    }

    private boolean isTimeout(RuntimeException exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        Throwable cause = exception.getCause();
        while (cause != null) {
            String className = cause.getClass().getName().toLowerCase();
            if (className.contains("timeout") || className.contains("timedout")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
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
                response.reviewMarkdown(),
                response.requiresDocumentInput()
        );
    }
}
