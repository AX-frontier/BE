package com.axprontier.api.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.SourceDto;
import com.axprontier.api.ai.dto.TargetAgent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpAiOrchestratorClientTest {

    @Test
    void callsExecutableOrchestratorChatWithSameIds() {
        UUID queryUid = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        UUID conversationUid = UUID.randomUUID();
        String message = "복수전공 신청 기간 알려줘";
        OrchestrateRequest request = new OrchestrateRequest(queryUid, traceId, conversationUid, message);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAiOrchestratorClient client = new HttpAiOrchestratorClient(
                builder.build(),
                "/library/chat",
                "/document-review/chat"
        );

        server.expect(once(), requestTo("http://localhost:8000/orchestrator/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queryUid").value(queryUid.toString()))
                .andExpect(jsonPath("$.traceId").value(traceId.toString()))
                .andExpect(jsonPath("$.conversationUid").value(conversationUid.toString()))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.document").doesNotExist())
                .andRespond(withSuccess(mainResponseJson(), MediaType.APPLICATION_JSON));

        OrchestrateResponse response = client.orchestrateChat(request);

        assertThat(response.targetAgent()).isEqualTo("MAIN");
        assertThat(response.answer()).isEqualTo(mainOfficialLinkAnswer());
        assertThat(response.sources()).containsExactly(mainOfficialSource());
        server.verify();
    }

    @Test
    void forwardsDocumentToExecutableOrchestratorChat() {
        UUID queryUid = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        UUID conversationUid = UUID.randomUUID();
        String message = "기안할 문서가 있는데 검토해줄 수 있어?";
        OrchestrateRequest request = new OrchestrateRequest(
                queryUid,
                traceId,
                conversationUid,
                message,
                Map.of(
                        "title", "문서 제목",
                        "docType", "OFFICIAL_DOCUMENT",
                        "bodyText", "검토할 문서 본문",
                        "bodyHtml", "<p>검토할 문서 본문</p>",
                        "editorJson", Map.of(),
                        "attachmentNames", List.of()
                )
        );

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAiOrchestratorClient client = new HttpAiOrchestratorClient(
                builder.build(),
                "/library/chat",
                "/document-review/chat"
        );

        server.expect(once(), requestTo("http://localhost:8000/orchestrator/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queryUid").value(queryUid.toString()))
                .andExpect(jsonPath("$.traceId").value(traceId.toString()))
                .andExpect(jsonPath("$.conversationUid").value(conversationUid.toString()))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.document.title").value("문서 제목"))
                .andExpect(jsonPath("$.document.docType").value("OFFICIAL_DOCUMENT"))
                .andExpect(jsonPath("$.document.bodyText").value("검토할 문서 본문"))
                .andExpect(jsonPath("$.document.bodyHtml").value("<p>검토할 문서 본문</p>"))
                .andExpect(jsonPath("$.document.editorJson").exists())
                .andExpect(jsonPath("$.document.attachmentNames").isArray())
                .andRespond(withSuccess(documentReviewResponseJson(), MediaType.APPLICATION_JSON));

        OrchestrateResponse response = client.orchestrateChat(request);

        assertThat(response.targetAgent()).isEqualTo("DOCUMENT_REVIEW");
        assertThat(response.answer()).isEqualTo("문서 검토 결과입니다.");
        server.verify();
    }

    @Test
    void legacyEndpointForDocumentReviewReturnsConfiguredPath() {
        HttpAiOrchestratorClient client = new HttpAiOrchestratorClient(
                RestClient.builder().baseUrl("http://localhost:8000").build(),
                "/library/chat",
                "/document-review/chat"
        );

        assertThat(client.endpointFor(TargetAgent.DOCUMENT_REVIEW)).isEqualTo("/document-review/chat");
    }

    private String mainResponseJson() {
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
                  "fallbackReason": null
                }
                """.formatted(jsonString(mainOfficialLinkAnswer()));
    }

    private String documentReviewResponseJson() {
        return """
                {
                  "targetAgent": "DOCUMENT_REVIEW",
                  "intent": "DOCUMENT_REVIEW",
                  "answer": "문서 검토 결과입니다.",
                  "sources": [],
                  "confidence": 0.88,
                  "fallbackUsed": false,
                  "fallbackReason": null
                }
                """;
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
