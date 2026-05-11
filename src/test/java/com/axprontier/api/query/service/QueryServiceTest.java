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
import com.axprontier.api.ai.dto.RouteEvidence;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.SourceDto;
import com.axprontier.api.ai.dto.TargetAgent;
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
    void storesOfficialLinkAnswerVerbatimAndPersistsOnlySourcesJsonFromAiResponse() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("학사 공지");
        QueryCreateRequest request = new QueryCreateRequest("복수전공 신청 기간 알려줘", "WEB");
        RouteResponse routeResponse = routeResponse(conversation);
        OrchestrateResponse mainResponse = mainResponse();

        when(conversationService.getByUid(conversationUid)).thenReturn(conversation);
        when(queryRepository.save(any(Query.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryRouteRepository.save(any(QueryRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryResponseRepository.save(any(QueryResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRequestLogRepository.save(any(AiRequestLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiResponseLogRepository.save(any(AiResponseLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.endpointFor(TargetAgent.MAIN)).thenReturn("/main/chat");
        when(aiGatewayService.chat(eq(TargetAgent.MAIN), any(OrchestrateRequest.class))).thenReturn(mainResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("MAIN");
        assertThat(response.answer()).isEqualTo(mainOfficialLinkAnswer());
        assertThat(response.sources()).containsExactly(mainOfficialSource());

        ArgumentCaptor<QueryResponse> queryResponseCaptor = ArgumentCaptor.forClass(QueryResponse.class);
        org.mockito.Mockito.verify(queryResponseRepository).save(queryResponseCaptor.capture());
        QueryResponse savedResponse = queryResponseCaptor.getValue();

        assertThat(ReflectionTestUtils.getField(savedResponse, "answerText"))
                .isEqualTo(mainOfficialLinkAnswer());
        assertThat(ReflectionTestUtils.getField(savedResponse, "sourceCount"))
                .isEqualTo(1);
        assertThat(ReflectionTestUtils.getField(savedResponse, "sourcesJson"))
                .isEqualTo(Map.of("sources", List.of(mainOfficialSource())));
    }

    @Test
    void savesLibrarySearchLogAndReturnsMatchedBooksWhenLibraryAgentResponds() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("도서관 질문");
        QueryCreateRequest request = new QueryCreateRequest("클린 코드 책 찾아줘", "WEB");
        RouteResponse routeResponse = libraryRouteResponse(conversation);
        OrchestrateResponse libraryResponse = libraryOrchestrateResponse();

        when(conversationService.getByUid(conversationUid)).thenReturn(conversation);
        when(queryRepository.save(any(Query.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryRouteRepository.save(any(QueryRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryResponseRepository.save(any(QueryResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRequestLogRepository.save(any(AiRequestLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiResponseLogRepository.save(any(AiResponseLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.endpointFor(TargetAgent.LIBRARY)).thenReturn("/library/chat");
        when(aiGatewayService.chat(eq(TargetAgent.LIBRARY), any(OrchestrateRequest.class))).thenReturn(libraryResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("LIBRARY");
        assertThat(response.searchKeyword()).isEqualTo("클린 코드");
        assertThat(response.resultCount()).isEqualTo(1);
        assertThat(response.matchedBooks()).containsExactly(sampleBook());
        verify(librarySearchLogService).saveIfLibrarySearch(any(Query.class), eq(libraryResponse));
    }

    @Test
    void skipsLibrarySearchLogWhenLibraryAgentReturnsFallback() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("도서관 질문");
        QueryCreateRequest request = new QueryCreateRequest("파이썬 책 어디 있어?", "WEB");
        RouteResponse routeResponse = libraryRouteResponse(conversation);
        OrchestrateResponse fallbackResponse = new OrchestrateResponse(
                "FALLBACK", "BOOK_SEARCH", "일시적인 오류가 발생했습니다.",
                List.of(), BigDecimal.ZERO, true, "AGENT_CALL_FAILED", null, null, null
        );

        when(conversationService.getByUid(conversationUid)).thenReturn(conversation);
        when(queryRepository.save(any(Query.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryRouteRepository.save(any(QueryRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryResponseRepository.save(any(QueryResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRequestLogRepository.save(any(AiRequestLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiResponseLogRepository.save(any(AiResponseLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.endpointFor(TargetAgent.LIBRARY)).thenReturn("/library/chat");
        when(aiGatewayService.chat(eq(TargetAgent.LIBRARY), any(OrchestrateRequest.class))).thenThrow(new RuntimeException("500 Internal Server Error"));
        when(aiGatewayService.fallbackResponse(routeResponse.intent(), "AGENT_CALL_FAILED")).thenReturn(fallbackResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("AGENT_CALL_FAILED");
        verify(librarySearchLogService).saveIfLibrarySearch(any(Query.class), eq(fallbackResponse));
    }

    @Test
    void returnsDocumentReviewGuideWithoutCallingAgentWhenGeneralChatRoutesToDocumentReview() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation("문서 검토 질문");
        QueryCreateRequest request = new QueryCreateRequest("전자결재 문서를 검토해줘", "WEB");
        RouteResponse routeResponse = documentReviewRouteResponse(conversation);
        OrchestrateResponse guideResponse = documentReviewGuideResponse(routeResponse);

        when(conversationService.getByUid(conversationUid)).thenReturn(conversation);
        when(queryRepository.save(any(Query.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryRouteRepository.save(any(QueryRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(queryResponseRepository.save(any(QueryResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRequestLogRepository.save(any(AiRequestLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiResponseLogRepository.save(any(AiResponseLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.documentReviewGuideResponse(routeResponse.intent(), routeResponse.confidence()))
                .thenReturn(guideResponse);

        QueryCreateResponse response = service.create(conversationUid, request);

        assertThat(response.targetAgent()).isEqualTo("DOCUMENT_REVIEW");
        assertThat(response.answer()).isEqualTo(documentReviewGuideAnswer());
        assertThat(response.sources()).isEmpty();
        assertThat(response.fallbackUsed()).isFalse();
        verify(aiGatewayService, never()).chat(eq(TargetAgent.DOCUMENT_REVIEW), any(OrchestrateRequest.class));

        ArgumentCaptor<QueryResponse> queryResponseCaptor = ArgumentCaptor.forClass(QueryResponse.class);
        org.mockito.Mockito.verify(queryResponseRepository).save(queryResponseCaptor.capture());
        QueryResponse savedResponse = queryResponseCaptor.getValue();
        assertThat(ReflectionTestUtils.getField(savedResponse, "answerText"))
                .isEqualTo(documentReviewGuideAnswer());
        assertThat(ReflectionTestUtils.getField(savedResponse, "sourceCount"))
                .isEqualTo(0);
        assertThat(ReflectionTestUtils.getField(savedResponse, "sourcesJson"))
                .isEqualTo(Map.of("sources", List.of()));
    }

    private RouteResponse routeResponse(Conversation conversation) {
        return new RouteResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                conversation.getConversationUid(),
                "MAIN",
                "ACADEMIC_NOTICE",
                BigDecimal.valueOf(0.84),
                "route selected targetAgent from orchestrator",
                new RouteEvidence(
                        BigDecimal.valueOf(0.72),
                        BigDecimal.valueOf(0.21),
                        BigDecimal.ZERO,
                        "main reranked hits: 2026학년도 1학기 복수·부전공 신청 및 변경신청 안내 (main-1, 0.720), 학사 공지 (main-2, 0.640)",
                        "library",
                        "document"
                )
        );
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

    private RouteResponse libraryRouteResponse(Conversation conversation) {
        return new RouteResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                conversation.getConversationUid(),
                "LIBRARY",
                "BOOK_SEARCH",
                BigDecimal.valueOf(0.92),
                "library reranked hits: 클린 코드 (lib-1, 0.920)",
                null
        );
    }

    private OrchestrateResponse libraryOrchestrateResponse() {
        return new OrchestrateResponse(
                "LIBRARY",
                "BOOK_SEARCH",
                "클린 코드 도서 1건을 찾았습니다.",
                List.of(new SourceDto(10L, "학술정보관", "https://library.example", "2026-05-10")),
                BigDecimal.valueOf(0.92),
                false,
                null,
                "클린 코드",
                1,
                List.of(sampleBook())
        );
    }

    private RouteResponse documentReviewRouteResponse(Conversation conversation) {
        return new RouteResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                conversation.getConversationUid(),
                "DOCUMENT_REVIEW",
                "DOCUMENT_REVIEW",
                BigDecimal.valueOf(0.88),
                "document review classifier matched",
                new RouteEvidence(
                        BigDecimal.valueOf(0.1),
                        BigDecimal.valueOf(0.2),
                        BigDecimal.valueOf(0.88),
                        "main reranked hits: none",
                        "library",
                        "document_review.routing classifier: review request"
                )
        );
    }

    private OrchestrateResponse documentReviewGuideResponse(RouteResponse routeResponse) {
        return new OrchestrateResponse(
                "DOCUMENT_REVIEW",
                routeResponse.intent(),
                documentReviewGuideAnswer(),
                List.of(),
                routeResponse.confidence(),
                false,
                null,
                null,
                null,
                null
        );
    }

    private String documentReviewGuideAnswer() {
        return "문서 검토는 문서 검토 화면에서 문서를 첨부하거나 본문을 입력한 뒤 진행해주세요.";
    }

    private MatchedBookDto sampleBook() {
        return new MatchedBookDto(
                10L,
                "BIB-1",
                "REG-1",
                "클린 코드",
                "Robert C. Martin",
                "인사이트",
                2013,
                "005.1 M381c",
                "단행본",
                "MAIN",
                "중앙도서관",
                "3층"
        );
    }
}
