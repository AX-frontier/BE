package com.axprontier.api.ai.service;

import com.axprontier.api.ai.client.AiOrchestratorClient;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiGatewayService {

    private final AiOrchestratorClient aiOrchestratorClient;

    public AiGatewayService(AiOrchestratorClient aiOrchestratorClient) {
        this.aiOrchestratorClient = aiOrchestratorClient;
    }

    public RouteResponse route(RouteRequest request) {
        return aiOrchestratorClient.route(request);
    }

    public OrchestrateResponse chat(TargetAgent targetAgent, OrchestrateRequest request) {
        if (targetAgent == TargetAgent.FALLBACK) {
            return fallbackResponse("FALLBACK", "NO_TARGET_AGENT");
        }
        return aiOrchestratorClient.chat(targetAgent, request);
    }

    public OrchestrateResponse fallbackResponse(String intent, String fallbackReason) {
        return new OrchestrateResponse(
                TargetAgent.FALLBACK.name(),
                intent,
                "질문을 처리하지 못했습니다. 학교, 도서관, 문서 검토와 관련된 질문으로 다시 입력해주세요.",
                List.of(),
                BigDecimal.ZERO,
                true,
                fallbackReason,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public String endpointFor(TargetAgent targetAgent) {
        return aiOrchestratorClient.endpointFor(targetAgent);
    }
}
