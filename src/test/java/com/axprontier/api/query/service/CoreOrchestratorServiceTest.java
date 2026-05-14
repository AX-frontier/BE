package com.axprontier.api.query.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class CoreOrchestratorServiceTest {

    private final AiGatewayService aiGatewayService = org.mockito.Mockito.mock(AiGatewayService.class);
    private final StreamQueryPersistenceService streamQueryPersistenceService = org.mockito.Mockito.mock(StreamQueryPersistenceService.class);
    private final CoreOrchestratorService service = new CoreOrchestratorService(
            aiGatewayService,
            streamQueryPersistenceService
    );

    @Test
    void recordsDocumentReviewResultAfterStreamCompletes() {
        CoreQueryRequest request = new CoreQueryRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "local-fe-user",
                "전자결재 문서를 검토해줘",
                Map.of("title", "전자결재 문서", "docType", "OFFICIAL_DOCUMENT", "bodyText", "본문")
        );
        SseEmitter emitter = new SseEmitter();
        OrchestrateResponse response = new OrchestrateResponse(
                "DOCUMENT_REVIEW",
                "DOCUMENT_REVIEW",
                "검토 결과입니다.",
                List.of(),
                BigDecimal.valueOf(0.88),
                false,
                null,
                null,
                null,
                null
        );
        when(aiGatewayService.streamOrchestrateChat(any(OrchestrateRequest.class), any(SseEmitter.class)))
                .thenReturn(response);

        service.queryStream(request, emitter);

        verify(streamQueryPersistenceService).saveCompleted(request, response);
    }
}
