package com.axprontier.api.ai.service;

import com.axprontier.api.ai.client.AiOrchestratorClient;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import org.springframework.stereotype.Service;

@Service
public class AiGatewayService {

    private final AiOrchestratorClient aiOrchestratorClient;

    public AiGatewayService(AiOrchestratorClient aiOrchestratorClient) {
        this.aiOrchestratorClient = aiOrchestratorClient;
    }

    public OrchestrateResponse orchestrate(OrchestrateRequest request) {
        return aiOrchestratorClient.orchestrate(request);
    }
}
