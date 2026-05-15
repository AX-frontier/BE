package com.axprontier.api.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.repository.ConversationRepository;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.review.dto.DocumentReviewRequest;
import com.axprontier.api.review.entity.ReviewFinding;
import com.axprontier.api.review.entity.ReviewRequest;
import com.axprontier.api.review.entity.ReviewResult;
import com.axprontier.api.review.repository.ReviewFindingRepository;
import com.axprontier.api.review.repository.ReviewRequestRepository;
import com.axprontier.api.review.repository.ReviewResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class DocumentReviewServiceTest {

    private final AiGatewayService aiGatewayService = org.mockito.Mockito.mock(AiGatewayService.class);
    private final ConversationRepository conversationRepository = org.mockito.Mockito.mock(ConversationRepository.class);
    private final QueryRepository queryRepository = org.mockito.Mockito.mock(QueryRepository.class);
    private final ReviewRequestRepository reviewRequestRepository = org.mockito.Mockito.mock(ReviewRequestRepository.class);
    private final ReviewResultRepository reviewResultRepository = org.mockito.Mockito.mock(ReviewResultRepository.class);
    private final ReviewFindingRepository reviewFindingRepository = org.mockito.Mockito.mock(ReviewFindingRepository.class);
    private final TransactionTemplate transactionTemplate = org.mockito.Mockito.mock(TransactionTemplate.class);

    private final DocumentReviewService service = new DocumentReviewService(
            aiGatewayService,
            conversationRepository,
            queryRepository,
            reviewRequestRepository,
            reviewResultRepository,
            reviewFindingRepository,
            transactionTemplate
    );

    @Test
    void directlyCallsDocumentReviewAgentAndPersistsReviewArtifacts() {
        UUID queryUid = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "전자결재 문서검토");
        Query query = new Query(queryUid, conversation, "전자결재 문서를 검토해줘", "DOCUMENT_REVIEW");
        ReviewRequest savedRequest = new ReviewRequest(query, "전자결재 문서", "OFFICIAL_DOCUMENT", "본문");
        ReviewResult savedResult = new ReviewResult(savedRequest, BigDecimal.valueOf(0.82), "검토 결과");
        DocumentReviewRequest request = new DocumentReviewRequest(
                queryUid,
                traceId,
                conversationUid,
                "user-1",
                "전자결재 문서를 검토해줘",
                Map.of(
                        "title", "전자결재 문서",
                        "docType", "OFFICIAL_DOCUMENT",
                        "bodyText", "본문",
                        "bodyHtml", "<p>본문</p>",
                        "editorJson", Map.of("type", "doc"),
                        "attachmentNames", List.of("첨부1.pdf")
                )
        );
        OrchestrateResponse aiResponse = documentReviewResponse();

        when(conversationRepository.findByConversationUid(conversationUid)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(queryRepository.findByQueryUid(queryUid)).thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenReturn(query);
        when(reviewRequestRepository.save(any(ReviewRequest.class))).thenReturn(savedRequest);
        when(reviewResultRepository.save(any(ReviewResult.class))).thenReturn(savedResult);
        when(reviewFindingRepository.save(any(ReviewFinding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(aiGatewayService.chat(eq(TargetAgent.DOCUMENT_REVIEW), any(OrchestrateRequest.class))).thenReturn(aiResponse);

        var response = service.review(request);

        assertThat(response.targetAgent()).isEqualTo("DOCUMENT_REVIEW");
        ArgumentCaptor<OrchestrateRequest> aiRequestCaptor = ArgumentCaptor.forClass(OrchestrateRequest.class);
        verify(aiGatewayService).chat(eq(TargetAgent.DOCUMENT_REVIEW), aiRequestCaptor.capture());
        assertThat(aiRequestCaptor.getValue().document()).isEqualTo(request.document());

        ArgumentCaptor<ReviewRequest> requestCaptor = ArgumentCaptor.forClass(ReviewRequest.class);
        verify(reviewRequestRepository).save(requestCaptor.capture());
        assertThat(ReflectionTestUtils.getField(requestCaptor.getValue(), "title")).isEqualTo("전자결재 문서");
        assertThat(ReflectionTestUtils.getField(requestCaptor.getValue(), "docType")).isEqualTo("OFFICIAL_DOCUMENT");
        assertThat(ReflectionTestUtils.getField(requestCaptor.getValue(), "bodyText")).isEqualTo("본문");

        ArgumentCaptor<ReviewFinding> findingCaptor = ArgumentCaptor.forClass(ReviewFinding.class);
        verify(reviewFindingRepository, times(2)).save(findingCaptor.capture());
        ReviewFinding firstFinding = findingCaptor.getAllValues().get(0);
        ReviewFinding secondFinding = findingCaptor.getAllValues().get(1);
        assertThat(ReflectionTestUtils.getField(firstFinding, "sectionType")).isEqualTo("날짜 표기");
        assertThat(ReflectionTestUtils.getField(firstFinding, "severity")).isEqualTo("MEDIUM");
        assertThat(ReflectionTestUtils.getField(firstFinding, "suggestion")).isEqualTo("2026. 4. 2.");
        assertThat(ReflectionTestUtils.getField(secondFinding, "sectionType")).isEqualTo("끝표시");
        assertThat(ReflectionTestUtils.getField(secondFinding, "lineStart")).isNull();
        assertThat(ReflectionTestUtils.getField(secondFinding, "lineEnd")).isNull();
    }

    @Test
    void deserializesTableChecksFromAiResponse() throws Exception {
        String payload = """
                {
                  "targetAgent": "DOCUMENT_REVIEW",
                  "intent": "DOCUMENT_REVIEW",
                  "answer": "검토 결과",
                  "sources": [],
                  "confidence": 0.82,
                  "fallbackUsed": false,
                  "unknownFutureField": "ignored",
                  "tableChecks": [
                    {
                      "id": "table-check-001",
                      "table_index": 1,
                      "table_title": "수입 정산 상세 내역",
                      "category": "표 검토",
                      "severity": "HIGH",
                      "status": "CHECK_REQUIRED",
                      "message": "금액 불일치",
                      "suggestion": "원본 표 확인",
                      "evidence": {"difference": 14920}
                    }
                  ],
                  "tableChecksAvailable": true,
                  "reviewMarkdown": "검토 결과",
                  "requiresDocumentInput": false
                }
                """;

        OrchestrateResponse response = new ObjectMapper().readValue(payload, OrchestrateResponse.class);

        assertThat(response.tableChecks()).hasSize(1);
        assertThat(response.tableChecks().get(0).tableIndex()).isEqualTo(1);
        assertThat(response.tableChecks().get(0).tableTitle()).isEqualTo("수입 정산 상세 내역");
        assertThat(response.tableChecksAvailable()).isTrue();
    }

    @Test
    void fallbackResponseMarksTableChecksUnavailable() {
        AiGatewayService gatewayService = new AiGatewayService(
                org.mockito.Mockito.mock(com.axprontier.api.ai.client.AiOrchestratorClient.class)
        );

        OrchestrateResponse response = gatewayService.fallbackResponse("FALLBACK", "AI_UNAVAILABLE");

        assertThat(response.tableChecks()).isEmpty();
        assertThat(response.tableChecksAvailable()).isFalse();
    }

    private OrchestrateResponse documentReviewResponse() {
        return new OrchestrateResponse(
                "DOCUMENT_REVIEW",
                "DOCUMENT_REVIEW",
                "검토 결과",
                List.of(),
                BigDecimal.valueOf(0.82),
                false,
                null,
                null,
                null,
                null,
                Map.of("totalFindingCount", 1),
                List.of(
                        Map.of(
                                "category", "날짜 표기",
                                "severity", "MEDIUM",
                                "originalText", "2026-04-02",
                                "suggestedText", "2026. 4. 2.",
                                "reason", "날짜 형식 수정",
                                "lineStart", 1,
                                "lineEnd", 1
                        ),
                        Map.of(
                                "category", "끝표시",
                                "severity", "LOW",
                                "originalText", "끝.",
                                "suggestedText", "끝.",
                                "reason", "위치 확인"
                        )
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                true,
                Map.of("content", "2026. 4. 2."),
                "검토 결과",
                false
        );
    }
}
