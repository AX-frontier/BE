package com.axprontier.api.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.axprontier.api.ai.dto.MatchedBookDto;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.SourceDto;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.dto.CoreQueryResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CoreOrchestratorServiceTest {

    private final AiGatewayService aiGatewayService = org.mockito.Mockito.mock(AiGatewayService.class);
    private final CoreOrchestratorService service = new CoreOrchestratorService(aiGatewayService);

    @Test
    @DisplayName("복수전공 질의는 /orchestrator/chat MAIN 응답을 그대로 반환한다")
    void returnsMainResponseFromExecutableOrchestrator() {
        CoreQueryRequest request = request("복수전공 신청 기간 알려줘");
        OrchestrateResponse mainResponse = mainResponse();

        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(mainResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("MAIN");
        assertThat(response.answer()).isEqualTo(mainOfficialLinkAnswer());
        assertThat(response.sources()).containsExactly(mainOfficialSource());
        assertThat(response.fallbackUsed()).isFalse();
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    @DisplayName("도서 질의는 /orchestrator/chat LIBRARY 응답을 그대로 반환한다")
    void returnsLibraryResponseFromExecutableOrchestrator() {
        CoreQueryRequest request = request("파이썬 책 어디 있어?");
        OrchestrateResponse libraryResponse = libraryResponse();

        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(libraryResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("LIBRARY");
        assertThat(response.searchKeyword()).isEqualTo("파이썬");
        assertThat(response.resultCount()).isEqualTo(1);
        assertThat(response.matchedBooks()).containsExactly(sampleBook());
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    @DisplayName("문서 bodyText가 있으면 document DTO를 포함해 /orchestrator/chat으로 전달한다")
    void forwardsDocumentToExecutableOrchestrator() {
        Map<String, Object> document = document("검토할 본문입니다.");
        CoreQueryRequest request = request("기안할 문서가 있는데 검토해줄 수 있어?", document);
        OrchestrateResponse documentReviewResponse = documentReviewResponse();

        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(documentReviewResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("DOCUMENT_REVIEW");
        assertThat(response.answer()).isEqualTo("문서 검토 결과입니다.");

        ArgumentCaptor<OrchestrateRequest> requestCaptor = ArgumentCaptor.forClass(OrchestrateRequest.class);
        verify(aiGatewayService).orchestrateChat(requestCaptor.capture());
        assertThat(requestCaptor.getValue().queryUid()).isEqualTo(request.queryUid());
        assertThat(requestCaptor.getValue().traceId()).isEqualTo(request.traceId());
        assertThat(requestCaptor.getValue().conversationUid()).isEqualTo(request.conversationUid());
        assertThat(requestCaptor.getValue().message()).isEqualTo(request.message());
        assertThat(requestCaptor.getValue().document()).isEqualTo(document);
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    @DisplayName("문서검토 질의에 document가 없으면 Python FALLBACK 응답을 그대로 반환한다")
    void returnsPythonFallbackWhenDocumentIsMissing() {
        CoreQueryRequest request = request("기안할 문서가 있는데 검토해줄 수 있어?");
        OrchestrateResponse fallbackResponse = fallbackResponse("DOCUMENT_BODY_REQUIRED");

        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("DOCUMENT_BODY_REQUIRED");
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    @Test
    @DisplayName("인사말은 Python FALLBACK 응답을 그대로 반환한다")
    void returnsPythonFallbackForGreeting() {
        CoreQueryRequest request = request("안녕");
        OrchestrateResponse fallbackResponse = fallbackResponse("LOW_CONFIDENCE");

        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("LOW_CONFIDENCE");
    }

    @Test
    @DisplayName("Python /orchestrator/chat 장애 시 Spring 내부 fallback 응답을 반환한다")
    void returnsSpringFallbackWhenExecutableOrchestratorFails() {
        CoreQueryRequest request = request("복수전공 신청 기간 알려줘");
        OrchestrateResponse fallbackResponse = fallbackResponse("ORCHESTRATOR_CHAT_FAILED");

        when(aiGatewayService.orchestrateChat(any(OrchestrateRequest.class))).thenThrow(new RuntimeException("timeout"));
        when(aiGatewayService.fallbackResponse("FALLBACK", "ORCHESTRATOR_CHAT_FAILED")).thenReturn(fallbackResponse);

        CoreQueryResponse response = service.query(request);

        assertThat(response.targetAgent()).isEqualTo("FALLBACK");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).isEqualTo("ORCHESTRATOR_CHAT_FAILED");
        verify(aiGatewayService, never()).chat(any(), any(OrchestrateRequest.class));
    }

    private CoreQueryRequest request(String message) {
        return request(message, null);
    }

    private CoreQueryRequest request(String message, Map<String, Object> document) {
        return new CoreQueryRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "user-1", message, document);
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
                "BOOK_SEARCH",
                "파이썬 도서 1건을 찾았습니다.",
                List.of(),
                BigDecimal.valueOf(0.91),
                false,
                null,
                "파이썬",
                1,
                List.of(sampleBook())
        );
    }

    private MatchedBookDto sampleBook() {
        return new MatchedBookDto(
                10L,
                "BIB-10",
                "REG-10",
                "파이썬",
                "홍길동",
                "한성출판",
                2024,
                "005.133",
                "BOOK",
                "LIB",
                "3층 자료실",
                "A-12"
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
}
