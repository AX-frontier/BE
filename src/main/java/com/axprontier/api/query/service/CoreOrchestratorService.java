package com.axprontier.api.query.service;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CoreOrchestratorService {

    private final AiGatewayService aiGatewayService;

    public CoreOrchestratorService(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public void queryStream(CoreQueryRequest request, SseEmitter emitter) {
        OrchestrateRequest orchestrateRequest = new OrchestrateRequest(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                request.message(),
                request.document()
        );
        try {
            aiGatewayService.streamOrchestrateChat(orchestrateRequest, emitter);
        } catch (RuntimeException e) {
            try {
                OrchestrateResponse fallback = aiGatewayService.fallbackResponse("FALLBACK", "ORCHESTRATOR_CHAT_FAILED");
                emitter.send(SseEmitter.event().data("{\"type\":\"done\",\"targetAgent\":\"FALLBACK\",\"fallbackUsed\":true,\"answer\":\"" + fallback.answer() + "\",\"requiresDocumentInput\":false}"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

}
