package com.axprontier.api.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
}
