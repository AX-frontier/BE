package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.TargetAgent;

public interface AiOrchestratorClient {

    String ROUTE_ENDPOINT = "/orchestrator/route";
    String ORCHESTRATOR_CHAT_ENDPOINT = "/orchestrator/chat";

    RouteResponse route(RouteRequest request);

    OrchestrateResponse orchestrateChat(OrchestrateRequest request);

    OrchestrateResponse chat(TargetAgent targetAgent, OrchestrateRequest request);

    String endpointFor(TargetAgent targetAgent);
}
