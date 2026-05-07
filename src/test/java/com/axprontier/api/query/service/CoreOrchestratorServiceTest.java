package com.axprontier.api.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.dto.CoreQueryResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoreOrchestratorServiceTest {

    private final AiGatewayService aiGatewayService = org.mockito.Mockito.mock(AiGatewayService.class);
    private final CoreOrchestratorService service = new CoreOrchestratorService(aiGatewayService);

    @Test
    void returnsFallbackWithoutAgentCallWhenRouteTargetIsFallback() {
        CoreQueryRequest request = request("안녕");
        RouteResponse routeResponse = routeResponse(request, "FALLBACK");
        OrchestrateResponse fallbackResponse = fallbackResponse("ROUTE_TARGET_FALLBACK");

        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.fallbackResponse(routeResponse.intent(), "ROUTE_TARGET_FALLBACK")).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("ROUTE_TARGET_FALLBACK");
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    void returnsFallbackWithoutAgentCallWhenRouteFails() {
        CoreQueryRequest request = request("복수전공 신청 기간 알려줘");
        OrchestrateResponse fallbackResponse = fallbackResponse("ROUTE_FAILED");

        when(aiGatewayService.route(any(RouteRequest.class))).thenThrow(new RuntimeException("timeout"));
        when(aiGatewayService.fallbackResponse("FALLBACK", "ROUTE_FAILED")).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("ROUTE_FAILED");
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    private CoreQueryRequest request(String message) {
        return new CoreQueryRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "user-1", message);
    }

    private RouteResponse routeResponse(CoreQueryRequest request, String targetAgent) {
        return new RouteResponse(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                targetAgent,
                "TEST_INTENT",
                BigDecimal.valueOf(0.8),
                "test",
                null
        );
    }

    private OrchestrateResponse fallbackResponse(String fallbackReason) {
        return new OrchestrateResponse(
                "FALLBACK",
                "FALLBACK",
                "fallback",
                List.of(),
                BigDecimal.ZERO,
                true,
                fallbackReason,
                null,
                null,
                null
        );
    }
}
