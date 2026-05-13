package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.TargetAgent;

public interface AiOrchestratorClient {

    String ROUTE_ENDPOINT = "/orchestrator/route";
    String ORCHESTRATOR_CHAT_ENDPOINT = "/orchestrator/chat";
    String ORCHESTRATOR_CHAT_STREAM_ENDPOINT = "/orchestrator/chat/stream";

    RouteResponse route(RouteRequest request);

    OrchestrateResponse orchestrateChat(OrchestrateRequest request);

    OrchestrateResponse chat(TargetAgent targetAgent, OrchestrateRequest request);

    OrchestrateResponse streamOrchestrateChat(OrchestrateRequest request, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter);

    String endpointFor(TargetAgent targetAgent);
}
