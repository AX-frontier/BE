package com.axprontier.api.ai.service;

import com.axprontier.api.ai.client.AiOrchestratorClient;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
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

    public OrchestrateResponse chat(TargetAgent targetAgent, OrchestrateRequest request) {
        if (targetAgent == TargetAgent.FALLBACK) {
            return new OrchestrateResponse(
                    TargetAgent.FALLBACK.name(),
                    "FALLBACK",
                    "질문을 이해하지 못했습니다. 학교, 도서관, 문서 검토와 관련된 질문으로 다시 입력해주세요.",
                    List.of(),
                    BigDecimal.ZERO,
                    true,
                    "NO_KEYWORD_MATCH",
                    null,
                    null,
                    null
            );
        }
        return aiOrchestratorClient.chat(targetAgent, request);
    }

    public String endpointFor(TargetAgent targetAgent) {
        return aiOrchestratorClient.endpointFor(targetAgent);
    }
}
