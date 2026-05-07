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
        assertThat(agentResponse.targetAgent()).isEqualTo(targetAgent);
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
                    "mainReason": "main",
                    "libraryReason": "library",
                    "documentReviewReason": "document"
                  }
                }
                """.formatted(queryUid, traceId, conversationUid, targetAgent);
    }

    private String agentResponseJson(String targetAgent) {
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
}
