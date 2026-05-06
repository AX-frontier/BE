package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpAiOrchestratorClient implements AiOrchestratorClient {

    private final RestClient restClient;
    private final String documentReviewPath;

    public HttpAiOrchestratorClient(
            @Value("${ai.server.base-url}") String aiServerBaseUrl,
            @Value("${ai.server.document-review-path}") String documentReviewPath
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServerBaseUrl)
                .build();
        this.documentReviewPath = documentReviewPath;
    }

    @Override
    public OrchestrateResponse chat(TargetAgent targetAgent, OrchestrateRequest request) {
        return restClient.post()
                .uri(endpointFor(targetAgent))
                .body(request)
                .retrieve()
                .body(OrchestrateResponse.class);
    }

    @Override
    public String endpointFor(TargetAgent targetAgent) {
        return switch (targetAgent) {
            case MAIN -> "/main/chat";
            case LIBRARY -> "/library/chat";
            case DOCUMENT_REVIEW -> documentReviewPath;
            case FALLBACK -> "";
        };
    }
}
