package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;

public interface AiOrchestratorClient {

    OrchestrateResponse chat(TargetAgent targetAgent, OrchestrateRequest request);

    String endpointFor(TargetAgent targetAgent);
}
