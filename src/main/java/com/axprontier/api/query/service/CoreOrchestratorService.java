package com.axprontier.api.query.service;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
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
        RouteRequest routeRequest = new RouteRequest(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                request.message()
        );

        RouteResponse routeResponse;
        try {
            routeResponse = aiGatewayService.route(routeRequest);
            log.info(
                    "core_orchestrator_route traceId={} targetAgent={} intent={} confidence={} statusCode={} timeout=false reason={}",
                    request.traceId(),
                    routeResponse.targetAgent(),
                    routeResponse.intent(),
                    routeResponse.confidence(),
                    200,
                    routeResponse.reason()
            );
        } catch (RuntimeException exception) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse("FALLBACK", "ROUTE_FAILED");
            log.warn(
                    "core_orchestrator_route traceId={} targetAgent={} statusCode={} timeout={} error={}",
                    request.traceId(),
                    TargetAgent.FALLBACK.name(),
                    statusCode(exception),
                    isTimeout(exception),
                    exception.getClass().getSimpleName()
            );
            logResult(request, null, fallbackResponse, "FAILED", startedAt, false, statusCode(exception), isTimeout(exception));
            return toCoreResponse(fallbackResponse);
        }

        TargetAgent targetAgent = TargetAgent.from(routeResponse.targetAgent());
        log.info(
                "core_orchestrator_agent_selected traceId={} finalAgent={} libraryChatCalled={}",
                request.traceId(),
                targetAgent.name(),
                targetAgent == TargetAgent.LIBRARY
        );
        if (targetAgent == TargetAgent.FALLBACK) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse(routeResponse.intent(), "ROUTE_TARGET_FALLBACK");
            logResult(request, routeResponse, fallbackResponse, "COMPLETED", startedAt, false, 200, false);
            return toCoreResponse(fallbackResponse);
        }
        if (targetAgent == TargetAgent.DOCUMENT_REVIEW) {
            OrchestrateResponse guideResponse = aiGatewayService.documentReviewGuideResponse(
                    routeResponse.intent(),
                    routeResponse.confidence()
            );
            logResult(request, routeResponse, guideResponse, "COMPLETED", startedAt, false, 200, false);
            return toCoreResponse(guideResponse);
        }

        OrchestrateRequest orchestrateRequest = routeRequest.toOrchestrateRequest();
        try {
            OrchestrateResponse response = aiGatewayService.chat(targetAgent, orchestrateRequest);
            log.info(
                    "core_orchestrator_agent_call traceId={} targetAgent={} libraryChatCalled={} statusCode={} timeout=false",
                    request.traceId(),
                    targetAgent.name(),
                    targetAgent == TargetAgent.LIBRARY,
                    200
            );
            logResult(request, routeResponse, response, "COMPLETED", startedAt, targetAgent == TargetAgent.LIBRARY, 200, false);
            return toCoreResponse(response);
        } catch (RuntimeException exception) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse(routeResponse.intent(), "AGENT_CALL_FAILED");
            log.warn(
                    "core_orchestrator_agent_call traceId={} targetAgent={} libraryChatCalled={} statusCode={} timeout={} error={}",
                    request.traceId(),
                    targetAgent.name(),
                    targetAgent == TargetAgent.LIBRARY,
                    statusCode(exception),
                    isTimeout(exception),
                    exception.getClass().getSimpleName()
            );
            logResult(request, routeResponse, fallbackResponse, "FAILED", startedAt, targetAgent == TargetAgent.LIBRARY, statusCode(exception), isTimeout(exception));
            return toCoreResponse(fallbackResponse);
        }
    }

    private void logResult(
            CoreQueryRequest request,
            RouteResponse routeResponse,
            OrchestrateResponse response,
            String status,
            Instant startedAt,
            boolean libraryChatCalled,
            int statusCode,
            boolean timeout
    ) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
                "core_orchestrator queryUid={} traceId={} conversationUid={} targetAgent={} intent={} status={} latencyMs={} fallbackUsed={} libraryChatCalled={} statusCode={} timeout={} routeReason={}",
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                response.targetAgent(),
                response.intent(),
                status,
                latencyMs,
                response.fallbackUsed(),
                libraryChatCalled,
                statusCode,
                timeout,
                routeResponse == null ? "-" : routeResponse.reason()
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
                response.reviewMarkdown()
        );
    }
}
