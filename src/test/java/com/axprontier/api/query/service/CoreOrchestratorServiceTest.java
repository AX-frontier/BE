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
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.SourceDto;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.dto.CoreQueryResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CoreOrchestratorServiceTest {

    private final AiGatewayService aiGatewayService = org.mockito.Mockito.mock(AiGatewayService.class);
    private final CoreOrchestratorService service = new CoreOrchestratorService(aiGatewayService);

    @Test
    @DisplayName("route targetAgent가 LIBRARY이면 /library/chat 대상 에이전트 호출로 이어지고 응답 필드가 보존된다")
    void callsLibraryAgentAndPreservesLibraryFieldsWhenRouteTargetIsLibrary() {
        CoreQueryRequest request = request("오늘 도서관 몇 시까지 열어요?");
        RouteResponse routeResponse = routeResponse(request, "LIBRARY");
        OrchestrateResponse libraryResponse = libraryResponse();

        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.chat(eq(TargetAgent.LIBRARY), any(OrchestrateRequest.class))).thenReturn(libraryResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("LIBRARY");
        assertThat(response.answer()).isEqualTo("오늘 도서관은 오후 9시까지 운영합니다.");
        assertThat(response.sources()).containsExactly(new SourceDto(1L, "도서관 운영시간", "https://library.example/hours", "2026-05-08"));
        assertThat(response.confidence()).isEqualByComparingTo("0.91");
        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.fallbackReason()).isNull();
        assertThat(response.searchKeyword()).isEqualTo("도서관 운영시간");
        assertThat(response.resultCount()).isEqualTo(1);
        assertThat(response.matchedBooks()).containsExactly(new MatchedBookDto(
                10L,
                "BIB-10",
                "REG-10",
                "Effective Java",
                "Joshua Bloch",
                "Addison-Wesley",
                2018,
                "005.133 B651e",
                "BOOK",
                "LIB",
                "3층 자료실",
                "A-12"
        ));

        ArgumentCaptor<OrchestrateRequest> requestCaptor = ArgumentCaptor.forClass(OrchestrateRequest.class);
        verify(aiGatewayService).chat(eq(TargetAgent.LIBRARY), requestCaptor.capture());
        assertThat(requestCaptor.getValue().queryUid()).isEqualTo(request.queryUid());
        assertThat(requestCaptor.getValue().traceId()).isEqualTo(request.traceId());
        assertThat(requestCaptor.getValue().conversationUid()).isEqualTo(request.conversationUid());
        assertThat(requestCaptor.getValue().message()).isEqualTo(request.message());
    }

    @Test
    @DisplayName("route targetAgent가 MAIN이면 Library Agent를 호출하지 않는다")
    void callsMainAgentAndDoesNotCallLibraryAgentWhenRouteTargetIsMain() {
        CoreQueryRequest request = request("복수전공 신청 기간 알려줘");
        RouteResponse routeResponse = routeResponse(request, "MAIN");
        OrchestrateResponse mainResponse = new OrchestrateResponse(
                "MAIN",
                "ACADEMIC_CALENDAR",
                mainOfficialLinkAnswer(),
                List.of(mainOfficialSource()),
                BigDecimal.valueOf(0.84),
                false,
                null,
                null,
                null,
                null
        );

        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.chat(eq(TargetAgent.MAIN), any(OrchestrateRequest.class))).thenReturn(mainResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("MAIN");
        assertThat(response.answer()).isEqualTo(mainOfficialLinkAnswer());
        assertThat(response.sources()).containsExactly(mainOfficialSource());
        assertThat(response.fallbackUsed()).isFalse();
        verify(aiGatewayService).chat(eq(TargetAgent.MAIN), any(OrchestrateRequest.class));
        verify(aiGatewayService, never()).chat(eq(TargetAgent.LIBRARY), any(OrchestrateRequest.class));
    }

    @Test
    @DisplayName("route targetAgent가 DOCUMENT_REVIEW이면 문서 본문 입력 요청만 반환하고 Agent를 호출하지 않는다")
    void returnsDocumentInputRequestWithoutAgentCallWhenRouteTargetIsDocumentReview() {
        CoreQueryRequest request = request("전자결재 문서 검토해줘");
        RouteResponse routeResponse = routeResponse(request, "DOCUMENT_REVIEW");

        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("DOCUMENT_REVIEW");
        assertThat(response.intent()).isEqualTo("DOCUMENT_REVIEW_REQUIRED");
        assertThat(response.requiresDocumentInput()).isTrue();
        assertThat(response.documentInputType()).isEqualTo("OFFICIAL_DOCUMENT");
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    @DisplayName("Python Library Agent timeout/500 등 호출 실패 시 fallback 응답을 반환한다")
    void returnsFallbackWhenLibraryAgentCallFails() {
        CoreQueryRequest request = request("파이썬 책 어디 있어?");
        RouteResponse routeResponse = routeResponse(request, "LIBRARY");
        OrchestrateResponse fallbackResponse = fallbackResponse("AGENT_CALL_FAILED");

        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.chat(eq(TargetAgent.LIBRARY), any(OrchestrateRequest.class))).thenThrow(new RuntimeException("500"));
        when(aiGatewayService.fallbackResponse(routeResponse.intent(), "AGENT_CALL_FAILED")).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("AGENT_CALL_FAILED");
    }

    @Test
    void returnsFallbackWithoutAgentCallWhenRouteTargetIsFallback() {
        CoreQueryRequest request = request("안녕");
        RouteResponse routeResponse = routeResponse(request, "FALLBACK");
        OrchestrateResponse fallbackResponse = fallbackResponse("ROUTE_TARGET_FALLBACK");

        when(aiGatewayService.route(any(RouteRequest.class))).thenReturn(routeResponse);
        when(aiGatewayService.fallbackResponse(routeResponse.intent(), "ROUTE_TARGET_FALLBACK")).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("ROUTE_TARGET_FALLBACK");
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    void returnsFallbackWithoutAgentCallWhenRouteFails() {
        CoreQueryRequest request = request("복수전공 신청 기간 알려줘");
        OrchestrateResponse fallbackResponse = fallbackResponse("ROUTE_FAILED");

        when(aiGatewayService.route(any(RouteRequest.class))).thenThrow(new RuntimeException("timeout"));
        when(aiGatewayService.fallbackResponse("FALLBACK", "ROUTE_FAILED")).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("ROUTE_FAILED");
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    private CoreQueryRequest request(String message) {
        return new CoreQueryRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "user-1", message);
    }

    private RouteResponse routeResponse(CoreQueryRequest request, String targetAgent) {
        return new RouteResponse(
                request.queryUid(),
                request.traceId(),
                request.conversationUid(),
                targetAgent,
                "TEST_INTENT",
                BigDecimal.valueOf(0.8),
                "test",
                null
        );
    }

    private OrchestrateResponse fallbackResponse(String fallbackReason) {
        return new OrchestrateResponse(
                "FALLBACK",
                "FALLBACK",
                "fallback",
                List.of(),
                BigDecimal.ZERO,
                true,
                fallbackReason,
                null,
                null,
                null
        );
    }

    private OrchestrateResponse libraryResponse() {
        return new OrchestrateResponse(
                "LIBRARY",
                "LIBRARY_HOURS",
                "오늘 도서관은 오후 9시까지 운영합니다.",
                List.of(new SourceDto(1L, "도서관 운영시간", "https://library.example/hours", "2026-05-08")),
                BigDecimal.valueOf(0.91),
                false,
                null,
                "도서관 운영시간",
                1,
                List.of(new MatchedBookDto(
                        10L,
                        "BIB-10",
                        "REG-10",
                        "Effective Java",
                        "Joshua Bloch",
                        "Addison-Wesley",
                        2018,
                        "005.133 B651e",
                        "BOOK",
                        "LIB",
                        "3층 자료실",
                        "A-12"
                ))
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
