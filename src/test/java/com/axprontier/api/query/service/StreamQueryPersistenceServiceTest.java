package com.axprontier.api.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axprontier.api.ai.dto.MatchedBookDto;
import com.axprontier.api.ai.dto.OrchestrateResponse;
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
    void savesLibraryBookMatchesIntoResponseMetadata() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "파이썬 책 추천", "local-fe-user");
        CoreQueryRequest request = request(conversationUid, "파이썬 책 추천해줘", null);
        Query query = new Query(request.queryUid(), conversation, request.message(), "WEB");
        OrchestrateResponse response = new OrchestrateResponse(
                "LIBRARY",
                "BOOK_SEARCH",
                "추천 도서를 찾았습니다.",
                List.of(),
                BigDecimal.valueOf(0.91),
                false,
                null,
                "파이썬",
                1,
                List.of(sampleBook())
        );
        when(conversationRepository.findByConversationUid(conversationUid)).thenReturn(Optional.of(conversation));
        when(queryRepository.findByQueryUid(request.queryUid())).thenReturn(Optional.empty());
        when(queryRepository.save(any(Query.class))).thenReturn(query);
        when(queryResponseRepository.findByQuery(query)).thenReturn(Optional.empty());

        service.saveCompleted(request, response);

        ArgumentCaptor<QueryResponse> responseCaptor = ArgumentCaptor.forClass(QueryResponse.class);
        verify(queryResponseRepository).save(responseCaptor.capture());
        Map<String, Object> sourcesJson = responseCaptor.getValue().getSourcesJson();
        assertThat(sourcesJson).containsKey("library");
        assertThat(sourcesJson.get("library")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> library = (Map<String, Object>) sourcesJson.get("library");
        assertThat(library)
                .containsEntry("searchKeyword", "파이썬")
                .containsEntry("resultCount", 1);
        assertThat(library.get("matchedBooks")).isEqualTo(List.of(sampleBook()));
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

    private MatchedBookDto sampleBook() {
        return new MatchedBookDto(
                10L,
                "BIB-1",
                "REG-1",
                "파이썬으로 코딩하는 물리",
                "송오영",
                "21세기사",
                2021,
                "005.133 ㅅ574ㅍ",
                "단행본",
                "LOC",
                "인문자연과학자료실(5F)",
                "3-A-4-a"
        );
    }
}
