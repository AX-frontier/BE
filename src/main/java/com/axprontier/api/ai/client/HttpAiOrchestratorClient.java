package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpAiOrchestratorClient implements AiOrchestratorClient {

    private final RestClient restClient;
    private final String libraryPath;
    private final String documentReviewPath;

    @Autowired
    public HttpAiOrchestratorClient(
            @Value("${ai.server.base-url}") String aiServerBaseUrl,
            @Value("${ai.server.library-path}") String libraryPath,
            @Value("${ai.server.document-review-path}") String documentReviewPath,
            @Value("${ai.server.timeout-seconds:60}") long timeoutSeconds,
            RestClient.Builder restClientBuilder
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder
                .baseUrl(aiServerBaseUrl)
                .requestFactory(requestFactory)
                .build();
        this.libraryPath = libraryPath;
        this.documentReviewPath = documentReviewPath;
    }

    HttpAiOrchestratorClient(RestClient restClient, String libraryPath, String documentReviewPath) {
        this.restClient = restClient;
        this.libraryPath = libraryPath;
        this.documentReviewPath = documentReviewPath;
    }

    @Override
    public RouteResponse route(RouteRequest request) {
        return restClient.post()
                .uri(ROUTE_ENDPOINT)
                .body(request)
                .retrieve()
                .body(RouteResponse.class);
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
            case LIBRARY -> libraryPath;
            case DOCUMENT_REVIEW -> documentReviewPath;
            case FALLBACK -> "";
        };
    }
}