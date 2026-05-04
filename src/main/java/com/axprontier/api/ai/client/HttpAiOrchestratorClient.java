package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpAiOrchestratorClient implements AiOrchestratorClient {

    private final RestClient restClient;

    public HttpAiOrchestratorClient(@Value("${ai.server.base-url}") String aiServerBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServerBaseUrl)
                .build();
    }

    @Override
    public OrchestrateResponse orchestrate(OrchestrateRequest request) {
        return restClient.post()
                .uri("/ai/orchestrate")
                .body(request)
                .retrieve()
                .body(OrchestrateResponse.class);
    }
}
