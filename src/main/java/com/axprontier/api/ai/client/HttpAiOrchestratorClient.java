package com.axprontier.api.ai.client;

import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class HttpAiOrchestratorClient implements AiOrchestratorClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAiOrchestratorClient.class);

    private final RestClient restClient;
    private final HttpClient streamingHttpClient;
    private final String aiServerBaseUrl;
    private final String libraryPath;
    private final String documentReviewPath;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        this.streamingHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.aiServerBaseUrl = aiServerBaseUrl;
        this.libraryPath = libraryPath;
        this.documentReviewPath = documentReviewPath;
    }

    HttpAiOrchestratorClient(RestClient restClient, String libraryPath, String documentReviewPath) {
        this.restClient = restClient;
        this.streamingHttpClient = HttpClient.newHttpClient();
        this.aiServerBaseUrl = "";
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
    public OrchestrateResponse orchestrateChat(OrchestrateRequest request) {
        return restClient.post()
                .uri(ORCHESTRATOR_CHAT_ENDPOINT)
                .body(request)
                .retrieve()
                .body(OrchestrateResponse.class);
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
    public OrchestrateResponse streamOrchestrateChat(OrchestrateRequest request, SseEmitter emitter) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiServerBaseUrl + ORCHESTRATOR_CHAT_STREAM_ENDPOINT))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<java.io.InputStream> response = streamingHttpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                OrchestrateResponse finalResponse = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        emitter.send(SseEmitter.event().data(data));
                        finalResponse = readDoneResponse(data, finalResponse);
                    }
                }
                emitter.complete();
                return finalResponse;
            }
        } catch (Exception e) {
            log.error("SSE stream failed: {} {}", e.getClass().getSimpleName(), e.getMessage(), e);
            emitter.completeWithError(e);
            return null;
        }
    }

    private OrchestrateResponse readDoneResponse(String data, OrchestrateResponse previous) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(data);
            if (!"done".equals(node.path("type").asText())) {
                return previous;
            }
            if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode) {
                objectNode.remove("type");
            }
            return objectMapper.treeToValue(node, OrchestrateResponse.class);
        } catch (Exception exception) {
            return previous;
        }
    }

    @Override
    public String endpointFor(TargetAgent targetAgent) {
        return switch (targetAgent) {
            case MAIN -> "/main/chat";
            case LIBRARY -> libraryPath;
            case DOCUMENT_REVIEW -> documentReviewPath;
            case CAMPUS_MAP -> "/campus-map/chat";
            case FALLBACK -> "";
        };
    }
}
