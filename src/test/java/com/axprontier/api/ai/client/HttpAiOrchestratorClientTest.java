package com.axprontier.api.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.SourceDto;
import com.axprontier.api.ai.dto.TargetAgent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpAiOrchestratorClientTest {

    @Test
    void routesThenCallsMainAgentWithSameIds() {
        assertRoutesThenCallsAgent("복수전공 신청 기간 알려줘", "MAIN", "/main/chat");
    }

    @Test
    void routesThenCallsLibraryAgentWithSameIds() {
        assertRoutesThenCallsAgent("파이썬 책 어디 있어?", "LIBRARY", "/library/chat");
    }

    @Test
    void routesThenCallsDocumentReviewAgentWithSameIds() {
        assertRoutesThenCallsAgent("이 공문 문장 검토해줘", "DOCUMENT_REVIEW", "/document-review/chat");
    }

    private void assertRoutesThenCallsAgent(String message, String targetAgent, String agentPath) {
        UUID queryUid = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        UUID conversationUid = UUID.randomUUID();
        RouteRequest request = new RouteRequest(queryUid, traceId, conversationUid, message);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAiOrchestratorClient client = new HttpAiOrchestratorClient(
                builder.build(),
                "/library/chat",
                "/document-review/chat"
        );

        server.expect(once(), requestTo("http://localhost:8000/orchestrator/route"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queryUid").value(queryUid.toString()))
                .andExpect(jsonPath("$.traceId").value(traceId.toString()))
                .andExpect(jsonPath("$.conversationUid").value(conversationUid.toString()))
                .andExpect(jsonPath("$.message").value(message))
                .andRespond(withSuccess(routeResponseJson(queryUid, traceId, conversationUid, targetAgent), MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://localhost:8000" + agentPath))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queryUid").value(queryUid.toString()))
                .andExpect(jsonPath("$.traceId").value(traceId.toString()))
                .andExpect(jsonPath("$.conversationUid").value(conversationUid.toString()))
                .andExpect(jsonPath("$.message").value(message))
                .andRespond(withSuccess(agentResponseJson(targetAgent), MediaType.APPLICATION_JSON));

        RouteResponse routeResponse = client.route(request);
        OrchestrateResponse agentResponse = client.chat(TargetAgent.from(routeResponse.targetAgent()), request.toOrchestrateRequest());

        assertThat(routeResponse.targetAgent()).isEqualTo(targetAgent);
        assertThat(routeResponse.evidence().mainReason())
                .startsWith("main reranked hits:");
        assertThat(agentResponse.targetAgent()).isEqualTo(targetAgent);
        if ("MAIN".equals(targetAgent)) {
            assertThat(agentResponse.answer()).isEqualTo(mainOfficialLinkAnswer());
            assertThat(agentResponse.sources()).containsExactly(mainOfficialSource());
        }
        server.verify();
    }

    private String routeResponseJson(UUID queryUid, UUID traceId, UUID conversationUid, String targetAgent) {
        return """
                {
                  "queryUid": "%s",
                  "traceId": "%s",
                  "conversationUid": "%s",
                  "targetAgent": "%s",
                  "intent": "TEST_INTENT",
                  "confidence": 0.888,
                  "reason": "test route",
                  "evidence": {
                    "mainScore": 0.888,
                    "libraryScore": 0.4,
                    "documentReviewScore": 0.0,
                    "mainReason": "main reranked hits: 2026학년도 1학기 복수·부전공 신청 및 변경신청 안내 (main-1, 0.720), 학사 공지 (main-2, 0.640)",
                    "libraryReason": "library",
                    "documentReviewReason": "document"
                  }
                }
                """.formatted(queryUid, traceId, conversationUid, targetAgent);
    }

    private String agentResponseJson(String targetAgent) {
        if ("MAIN".equals(targetAgent)) {
            return """
                    {
                      "targetAgent": "MAIN",
                      "intent": "ACADEMIC_NOTICE",
                      "answer": %s,
                      "sources": [
                        {
                          "id": 219610,
                          "title": "2026학년도 1학기 복수·부전공 신청 및 변경신청 안내",
                          "sourceUrl": "https://www.hansung.ac.kr/bbs/hansung/2127/219610/artclView.do",
                          "updatedAt": "2026-05-08"
                        }
                      ],
                      "confidence": 0.9,
                      "fallbackUsed": false,
                      "fallbackReason": null,
                      "searchKeyword": null,
                      "resultCount": null,
                      "matchedBooks": null
                    }
                    """.formatted(jsonString(mainOfficialLinkAnswer()));
        }
        return """
                {
                  "targetAgent": "%s",
                  "intent": "TEST_INTENT",
                  "answer": "answer",
                  "sources": [],
                  "confidence": 0.9,
                  "fallbackUsed": false,
                  "fallbackReason": null,
                  "searchKeyword": null,
                  "resultCount": null,
                  "matchedBooks": null
                }
                """.formatted(targetAgent);
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

    private String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }
}
