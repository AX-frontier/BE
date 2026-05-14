package com.axprontier.api.query.service;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CoreOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(CoreOrchestratorService.class);

    private final AiGatewayService aiGatewayService;
    private final StreamQueryPersistenceService streamQueryPersistenceService;

    public CoreOrchestratorService(
            AiGatewayService aiGatewayService,
            StreamQueryPersistenceService streamQueryPersistenceService
    ) {
        this.aiGatewayService = aiGatewayService;
        this.streamQueryPersistenceService = streamQueryPersistenceService;
    }

    public void queryStream(CoreQueryRequest request, SseEmitter emitter) {
        OrchestrateRequest orchestrateRequest = new OrchestrateRequest(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                request.message(),
                request.document()
        );
        OrchestrateResponse response;
        try {
            streamQueryPersistenceService.validateConversationAccess(request);
            response = aiGatewayService.streamOrchestrateChat(orchestrateRequest, emitter);
        } catch (RuntimeException e) {
            try {
                OrchestrateResponse fallback = aiGatewayService.fallbackResponse("FALLBACK", "ORCHESTRATOR_CHAT_FAILED");
                emitter.send(SseEmitter.event().data("{\"type\":\"done\",\"targetAgent\":\"FALLBACK\",\"fallbackUsed\":true,\"answer\":\"" + fallback.answer() + "\",\"requiresDocumentInput\":false}"));
                streamQueryPersistenceService.saveCompleted(request, fallback);
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
            return;
        }
        try {
            streamQueryPersistenceService.saveCompleted(request, response);
        } catch (RuntimeException exception) {
            log.error(
                    "stream query persistence failed queryUid={} conversationUid={} error={}",
                    request.queryUid(),
                    request.conversationUid(),
                    exception.getClass().getSimpleName()
            );
        }
    }

}
