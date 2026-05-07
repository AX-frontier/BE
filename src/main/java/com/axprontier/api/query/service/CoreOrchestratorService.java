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
        } catch (RuntimeException exception) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse("FALLBACK", "ROUTE_FAILED");
            logResult(request, null, fallbackResponse, "FAILED", startedAt);
            return toCoreResponse(fallbackResponse);
        }

        TargetAgent targetAgent = TargetAgent.from(routeResponse.targetAgent());
        if (targetAgent == TargetAgent.FALLBACK) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse(routeResponse.intent(), "ROUTE_TARGET_FALLBACK");
            logResult(request, routeResponse, fallbackResponse, "COMPLETED", startedAt);
            return toCoreResponse(fallbackResponse);
        }

        OrchestrateRequest orchestrateRequest = routeRequest.toOrchestrateRequest();
        try {
            OrchestrateResponse response = aiGatewayService.chat(targetAgent, orchestrateRequest);
            logResult(request, routeResponse, response, "COMPLETED", startedAt);
            return toCoreResponse(response);
        } catch (RuntimeException exception) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse(routeResponse.intent(), "AGENT_CALL_FAILED");
            logResult(request, routeResponse, fallbackResponse, "FAILED", startedAt);
            return toCoreResponse(fallbackResponse);
        }
    }

    private void logResult(
            CoreQueryRequest request,
            RouteResponse routeResponse,
            OrchestrateResponse response,
            String status,
            Instant startedAt
    ) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
                "core_orchestrator queryUid={} traceId={} conversationUid={} targetAgent={} intent={} status={} latencyMs={} fallbackUsed={} routeReason={}",
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                response.targetAgent(),
                response.intent(),
                status,
                latencyMs,
                response.fallbackUsed(),
                routeResponse == null ? "-" : routeResponse.reason()
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
                response.matchedBooks()
        );
    }
}
