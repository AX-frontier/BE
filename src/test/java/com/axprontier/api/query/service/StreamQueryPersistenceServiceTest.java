package com.axprontier.api.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TableCheckDto;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.repository.ConversationRepository;
import com.axprontier.api.global.apiPayload.exception.GeneralException;
import com.axprontier.api.library.service.LibrarySearchLogService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.entity.QueryResponse;
import com.axprontier.api.query.repository.AgentRunRepository;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.query.repository.QueryResponseRepository;
import com.axprontier.api.query.repository.QueryRouteRepository;
import com.axprontier.api.review.service.DocumentReviewService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StreamQueryPersistenceServiceTest {

    private final ConversationRepository conversationRepository = org.mockito.Mockito.mock(ConversationRepository.class);
    private final QueryRepository queryRepository = org.mockito.Mockito.mock(QueryRepository.class);
    private final QueryRouteRepository queryRouteRepository = org.mockito.Mockito.mock(QueryRouteRepository.class);
    private final AgentRunRepository agentRunRepository = org.mockito.Mockito.mock(AgentRunRepository.class);
    private final QueryResponseRepository queryResponseRepository = org.mockito.Mockito.mock(QueryResponseRepository.class);
    private final LibrarySearchLogService librarySearchLogService = org.mockito.Mockito.mock(LibrarySearchLogService.class);
    private final DocumentReviewService documentReviewService = org.mockito.Mockito.mock(DocumentReviewService.class);
    private final AiGatewayService aiGatewayService = org.mockito.Mockito.mock(AiGatewayService.class);

    private final StreamQueryPersistenceService service = new StreamQueryPersistenceService(
            conversationRepository,
            queryRepository,
            queryRouteRepository,
            agentRunRepository,
            queryResponseRepository,
            librarySearchLogService,
            documentReviewService,
            aiGatewayService
    );

    @Test
    void savesStreamQueryResponseAndRelatedLogs() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "학사 질문", "local-fe-user");
        CoreQueryRequest request = request(conversationUid, "복수전공 신청 기간 알려줘", null);
        Query query = new Query(request.queryUid(), conversation, request.message(), "WEB");
        OrchestrateResponse response = response("MAIN", "SCHOOL_NOTICE_QA", "답변입니다.", false);
        when(conversationRepository.findByConversationUid(conversationUid)).thenReturn(Optional.of(conversation));
        when(queryRepository.findByQueryUid(request.queryUid())).thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenReturn(query);
        when(queryResponseRepository.findByQuery(query)).thenReturn(Optional.empty());

        service.saveCompleted(request, response);

        verify(queryRepository).save(any(Query.class));
        verify(queryRouteRepository).save(any());
        verify(agentRunRepository).save(any());
        verify(queryResponseRepository).save(any(QueryResponse.class));
        verify(librarySearchLogService).saveIfLibrarySearch(query, response);
        verify(documentReviewService).recordStreamedReview(request, response);
    }

    @Test
    void doesNotPersistDuplicateResponseForSameQueryUid() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "학사 질문", "local-fe-user");
        CoreQueryRequest request = request(conversationUid, "복수전공 신청 기간 알려줘", null);
        Query query = new Query(request.queryUid(), conversation, request.message(), "WEB");
        QueryResponse existingResponse = new QueryResponse(query, "기존 답변", Map.of(), 0, BigDecimal.ONE, null);
        when(conversationRepository.findByConversationUid(conversationUid)).thenReturn(Optional.of(conversation));
        when(queryRepository.findByQueryUid(request.queryUid())).thenReturn(Optional.of(query));
        when(queryResponseRepository.findByQuery(query)).thenReturn(Optional.of(existingResponse));

        service.saveCompleted(request, response("MAIN", "SCHOOL_NOTICE_QA", "새 답변", false));

        verify(queryResponseRepository, never()).save(any(QueryResponse.class));
        verify(queryRouteRepository, never()).save(any());
        verify(agentRunRepository, never()).save(any());
    }

    @Test
    void rejectsConversationOwnedByDifferentUser() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "학사 질문", "other-user");
        CoreQueryRequest request = request(conversationUid, "복수전공 신청 기간 알려줘", null);
        when(conversationRepository.findByConversationUid(conversationUid)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.saveCompleted(request, response("MAIN", "SCHOOL_NOTICE_QA", "답변", false)))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void persistsDocumentReviewMetadataForConversationRestore() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "전자결재 문서", "local-fe-user");
        Map<String, Object> document = Map.of(
                "bodyText", "붙임  1. 안내문 1부.  끝.",
                "bodyHtml", "<p>붙임  1. 안내문 1부.  끝.</p>"
        );
        CoreQueryRequest request = request(conversationUid, "전자결재 문서를 검토해줘", document);
        Query query = new Query(request.queryUid(), conversation, request.message(), "WEB");
        OrchestrateResponse response = documentReviewResponse();
        when(conversationRepository.findByConversationUid(conversationUid)).thenReturn(Optional.of(conversation));
        when(queryRepository.findByQueryUid(request.queryUid())).thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenReturn(query);
        when(queryResponseRepository.findByQuery(query)).thenReturn(Optional.empty());
        ArgumentCaptor<QueryResponse> responseCaptor = ArgumentCaptor.forClass(QueryResponse.class);

        service.saveCompleted(request, response);

        verify(queryResponseRepository).save(responseCaptor.capture());
        Map<String, Object> metadata = responseCaptor.getValue().getSourcesJson();
        assertThat(metadata).containsKey("documentReview");
        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) metadata.get("documentReview");
        assertThat(review.get("originalText")).isEqualTo(document.get("bodyText"));
        assertThat(review.get("originalHtml")).isEqualTo(document.get("bodyHtml"));
        assertThat(review.get("reviewMarkdown")).isEqualTo("검토 결과");
        assertThat(review.get("tableChecksAvailable")).isEqualTo(true);
        assertThat((List<?>) review.get("tableChecks")).hasSize(1);
    }

    private CoreQueryRequest request(UUID conversationUid, String message, Map<String, Object> document) {
        return new CoreQueryRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                conversationUid,
                "local-fe-user",
                message,
                document
        );
    }

    private OrchestrateResponse response(String targetAgent, String intent, String answer, boolean fallbackUsed) {
        return new OrchestrateResponse(
                targetAgent,
                intent,
                answer,
                List.of(),
                BigDecimal.valueOf(0.9),
                fallbackUsed,
                fallbackUsed ? "TEST_FALLBACK" : null,
                null,
                null,
                null
        );
    }

    private OrchestrateResponse documentReviewResponse() {
        return new OrchestrateResponse(
                "DOCUMENT_REVIEW",
                "DOCUMENT_REVIEW",
                "문서 검토 결과",
                List.of(),
                BigDecimal.valueOf(0.9),
                false,
                null,
                null,
                null,
                null,
                Map.of("totalFindingCount", 1, "highCount", 0, "mediumCount", 0, "lowCount", 1),
                List.of(Map.of("ruleCode", "END_MARKER")),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(new TableCheckDto(
                        "table-check-1",
                        1,
                        "표 1",
                        "소요예산",
                        "MEDIUM",
                        "CHECK_REQUIRED",
                        "소요예산 표 확인이 필요합니다.",
                        "원본 표에서 직접 확인해 주세요.",
                        Map.of("missingColumns", List.of("세목코드"))
                )),
                true,
                Map.of("format", "plain_text", "content", "수정 본문", "htmlContent", "<p>수정 본문</p>"),
                "검토 결과",
                false
        );
    }
}
