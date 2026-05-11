package com.axprontier.api.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axprontier.api.ai.dto.MatchedBookDto;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.SourceDto;
import com.axprontier.api.ai.entity.AiRequestLog;
import com.axprontier.api.ai.entity.AiResponseLog;
import com.axprontier.api.ai.repository.AiRequestLogRepository;
import com.axprontier.api.ai.repository.AiResponseLogRepository;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.service.ConversationService;
import com.axprontier.api.library.service.LibrarySearchLogService;
import com.axprontier.api.query.dto.QueryCreateRequest;
import com.axprontier.api.query.dto.QueryCreateResponse;
import com.axprontier.api.query.entity.AgentRun;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.entity.QueryResponse;
import com.axprontier.api.query.entity.QueryRoute;
import com.axprontier.api.query.repository.AgentRunRepository;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.query.repository.QueryResponseRepository;
import com.axprontier.api.query.repository.QueryRouteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class QueryServiceTest {

    private final ConversationService conversationService = org.mockito.Mockito.mock(ConversationService.class);
    private final QueryRepository queryRepository = org.mockito.Mockito.mock(QueryRepository.class);
    private final QueryRouteRepository queryRouteRepository = org.mockito.Mockito.mock(QueryRouteRepository.class);
    private final AgentRunRepository agentRunRepository = org.mockito.Mockito.mock(AgentRunRepository.class);
    private final QueryResponseRepository queryResponseRepository = org.mockito.Mockito.mock(QueryResponseRepository.class);
    private final AiRequestLogRepository aiRequestLogRepository = org.mockito.Mockito.mock(AiRequestLogRepository.class);
    private final AiResponseLogRepository aiResponseLogRepository = org.mockito.Mockito.mock(AiResponseLogRepository.class);
    private final AiGatewayService aiGatewayService = org.mockito.Mockito.mock(AiGatewayService.class);
    private final LibrarySearchLogService librarySearchLogService = org.mockito.Mockito.mock(LibrarySearchLogService.class);

    private final QueryService service = new QueryService(
            conversationService,
            queryRepository,
            queryRouteRepository,
            agentRunRepository,
            queryResponseRepository,
            aiRequestLogRepository,
            aiResponseLogRepository,
            aiGatewayService,
            librarySearchLogService,
            new ObjectMapper()
    );

    @Test
    void storesMainAnswerFromExecutableOrchestratorWithoutLegacyAgentCall() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("학사 공지");
        QueryCreateRequest request = new QueryCreateRequest("복수전공 신청 기간 알려줘", "WEB");
        OrchestrateResponse mainResponse = mainResponse();
        arrangePersistence(conversationUid, conversation);
        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(mainResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("MAIN");
        assertThat(response.answer()).isEqualTo(mainOfficialLinkAnswer());
        assertThat(response.sources()).containsExactly(mainOfficialSource());
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));

        ArgumentCaptor<QueryResponse> queryResponseCaptor = ArgumentCaptor.forClass(QueryResponse.class);
        verify(queryResponseRepository).save(queryResponseCaptor.capture());
        QueryResponse savedResponse = queryResponseCaptor.getValue();
        assertThat(ReflectionTestUtils.getField(savedResponse, "answerText")).isEqualTo(mainOfficialLinkAnswer());
        assertThat(ReflectionTestUtils.getField(savedResponse, "sourceCount")).isEqualTo(1);
        assertThat(ReflectionTestUtils.getField(savedResponse, "sourcesJson"))
                .isEqualTo(Map.of("sources", List.of(mainOfficialSource())));
    }

    @Test
    void savesLibrarySearchLogWhenExecutableOrchestratorReturnsLibrary() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("도서관 질문");
        QueryCreateRequest request = new QueryCreateRequest("파이썬 책 어디 있어?", "WEB");
        OrchestrateResponse libraryResponse = libraryOrchestrateResponse();
        arrangePersistence(conversationUid, conversation);
        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(libraryResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("LIBRARY");
        assertThat(response.searchKeyword()).isEqualTo("파이썬");
        assertThat(response.resultCount()).isEqualTo(1);
        assertThat(response.matchedBooks()).containsExactly(sampleBook());
        verify(librarySearchLogService).saveIfLibrarySearch(any(Query.class), eq(libraryResponse));
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    void forwardsDocumentDtoToExecutableOrchestratorAndStoresDocumentReviewResponse() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("문서 검토 질문");
        Map<String, Object> document = document("검토할 문서 본문");
        QueryCreateRequest request = new QueryCreateRequest("기안할 문서가 있는데 검토해줄 수 있어?", "WEB", document);
        OrchestrateResponse documentReviewResponse = documentReviewResponse();
        arrangePersistence(conversationUid, conversation);
        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(documentReviewResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("DOCUMENT_REVIEW");
        assertThat(response.answer()).isEqualTo("문서 검토 결과입니다.");
        assertThat(response.fallbackUsed()).isFalse();

        ArgumentCaptor<OrchestrateRequest> aiRequestCaptor = ArgumentCaptor.forClass(OrchestrateRequest.class);
        verify(aiGatewayService).orchestrateChat(aiRequestCaptor.capture());
        assertThat(aiRequestCaptor.getValue().document()).isEqualTo(document);
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    void returnsPythonFallbackResponseAsIs() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("일반 질문");
        QueryCreateRequest request = new QueryCreateRequest("안녕", "WEB");
        OrchestrateResponse fallbackResponse = fallbackResponse("LOW_CONFIDENCE");
        arrangePersistence(conversationUid, conversation);
        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(fallbackResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("LOW_CONFIDENCE");
        verify(aiGatewayService, never()).fallbackResponse(any(), any());
    }

    @Test
    void returnsSpringFallbackWhenExecutableOrchestratorFails() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("학사 공지");
        QueryCreateRequest request = new QueryCreateRequest("복수전공 신청 기간 알려줘", "WEB");
        OrchestrateResponse fallbackResponse = fallbackResponse("ORCHESTRATOR_CHAT_FAILED");
        arrangePersistence(conversationUid, conversation);
        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenThrow(new RuntimeException("timeout"));
        when(aiGatewayService.fallbackResponse("FALLBACK", "ORCHESTRATOR_CHAT_FAILED")).thenReturn(fallbackResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("ORCHESTRATOR_CHAT_FAILED");
    }

    private void arrangePersistence(UUID conversationUid, Conversation conversation) {
        when(conversationService.getByUid(conversationUid)).thenReturn(conversation);
        when(queryRepository.save(any(Query.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryRouteRepository.save(any(QueryRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryResponseRepository.save(any(QueryResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRequestLogRepository.save(any(AiRequestLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiResponseLogRepository.save(any(AiResponseLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private OrchestrateResponse mainResponse() {
        return new OrchestrateResponse(
                "MAIN",
                "ACADEMIC_NOTICE",
                mainOfficialLinkAnswer(),
                List.of(mainOfficialSource()),
                BigDecimal.valueOf(0.9),
                false,
                null,
                null,
                null,
                null
        );
    }

    private OrchestrateResponse libraryOrchestrateResponse() {
        return new OrchestrateResponse(
                "LIBRARY",
                "BOOK_SEARCH",
                "파이썬 도서 1건을 찾았습니다.",
                List.of(new SourceDto(10L, "학술정보관", "https://library.example", "2026-05-10")),
                BigDecimal.valueOf(0.92),
                false,
                null,
                "파이썬",
                1,
                List.of(sampleBook())
        );
    }

    private OrchestrateResponse documentReviewResponse() {
        return new OrchestrateResponse(
                "DOCUMENT_REVIEW",
                "DOCUMENT_REVIEW",
                "문서 검토 결과입니다.",
                List.of(),
                BigDecimal.valueOf(0.88),
                false,
                null,
                null,
                null,
                null
        );
    }

    private OrchestrateResponse fallbackResponse(String fallbackReason) {
        return new OrchestrateResponse(
                "FALLBACK",
                "FALLBACK",
                "질문을 처리하지 못했습니다.",
                List.of(),
                BigDecimal.ZERO,
                true,
                fallbackReason,
                null,
                null,
                null
        );
    }

    private Map<String, Object> document(String bodyText) {
        return Map.of(
                "title", "문서 제목",
                "docType", "OFFICIAL_DOCUMENT",
                "bodyText", bodyText,
                "bodyHtml", "<p>" + bodyText + "</p>",
                "editorJson", Map.of(),
                "attachmentNames", List.of()
        );
    }

    private String mainOfficialLinkAnswer() {
        return """
                "복수전공 신청 기간"와 관련해 확인할 수 있는 공식 링크를 찾았습니다.
                아래 링크들은 검색 결과에서 유사도가 높은 한성대학교 공지입니다.

                1. 2026학년도 1학기 복수·부전공 신청 및 변경신청 안내
                   복수·부전공 신청 안내에 대한 답변입니다.
                   이동하시려면 아래 링크를 눌러주세요.
                   https://www.hansung.ac.kr/bbs/hansung/2127/219610/artclView.do
                """;
    }

    private SourceDto mainOfficialSource() {
        return new SourceDto(
                219610L,
                "2026학년도 1학기 복수·부전공 신청 및 변경신청 안내",
                "https://www.hansung.ac.kr/bbs/hansung/2127/219610/artclView.do",
                "2026-05-08"
        );
    }

    private MatchedBookDto sampleBook() {
        return new MatchedBookDto(
                10L,
                "BIB-1",
                "REG-1",
                "파이썬",
                "홍길동",
                "한성출판",
                2024,
                "005.133",
                "단행본",
                "MAIN",
                "중앙도서관",
                "3층"
        );
    }
}
