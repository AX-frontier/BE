package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;

public interface AiOrchestratorClient {

    OrchestrateResponse orchestrate(OrchestrateRequest request);
}
