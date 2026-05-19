package com.axprontier.api.query.service;

import com.axprontier.api.ai.client.AiOrchestratorClient;
import com.axprontier.api.ai.entity.AiRequestLog;
import com.axprontier.api.ai.entity.AiResponseLog;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.repository.AiRequestLogRepository;
import com.axprontier.api.ai.repository.AiResponseLogRepository;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.service.ConversationService;
import com.axprontier.api.library.service.LibrarySearchLogService;
import com.axprontier.api.query.entity.AgentRun;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.entity.QueryResponse;
import com.axprontier.api.query.entity.QueryRoute;
import com.axprontier.api.query.dto.QueryCreateRequest;
import com.axprontier.api.query.dto.QueryCreateResponse;
import com.axprontier.api.query.repository.AgentRunRepository;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.query.repository.QueryResponseRepository;
import com.axprontier.api.query.repository.QueryRouteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private final ConversationService conversationService;
    private final QueryRepository queryRepository;
    private final QueryRouteRepository queryRouteRepository;
    private final AgentRunRepository agentRunRepository;
    private final QueryResponseRepository queryResponseRepository;
    private final AiRequestLogRepository aiRequestLogRepository;
    private final AiResponseLogRepository aiResponseLogRepository;
    private final AiGatewayService aiGatewayService;
    private final LibrarySearchLogService librarySearchLogService;
    private final ObjectMapper objectMapper;

    public QueryService(
            ConversationService conversationService,
            QueryRepository queryRepository,
            QueryRouteRepository queryRouteRepository,
            AgentRunRepository agentRunRepository,
            QueryResponseRepository queryResponseRepository,
            AiRequestLogRepository aiRequestLogRepository,
            AiResponseLogRepository aiResponseLogRepository,
            AiGatewayService aiGatewayService,
            LibrarySearchLogService librarySearchLogService,
            ObjectMapper objectMapper
    ) {
        this.conversationService = conversationService;
        this.queryRepository = queryRepository;
        this.queryRouteRepository = queryRouteRepository;
        this.agentRunRepository = agentRunRepository;
        this.queryResponseRepository = queryResponseRepository;
        this.aiRequestLogRepository = aiRequestLogRepository;
        this.aiResponseLogRepository = aiResponseLogRepository;
        this.aiGatewayService = aiGatewayService;
        this.librarySearchLogService = librarySearchLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QueryCreateResponse create(UUID conversationUid, QueryCreateRequest request) {
        Conversation conversation = conversationService.getByUid(conversationUid);
        Query query = queryRepository.save(new Query(conversation, request.message(), request.channel()));
        UUID traceId = UUID.randomUUID();

        OrchestrateRequest orchestrateRequest = new OrchestrateRequest(
                query.getQueryUid(),
                traceId,
                conversation.getConversationUid(),
                query.getQueryText(),
                request.document()
        );
        AgentResult agentResult = callOrchestrator(query, orchestrateRequest);
        OrchestrateResponse aiResponse = agentResult.response();
        String status = "COMPLETED";
        if (aiResponse == null) {
            status = "FAILED";
            aiResponse = aiGatewayService.fallbackResponse("FALLBACK", "ORCHESTRATOR_CHAT_FAILED");
        }

        TargetAgent targetAgent = TargetAgent.from(aiResponse.targetAgent());
        queryRouteRepository.save(new QueryRoute(
                query,
                aiResponse.intent(),
                targetAgent.name(),
                aiResponse.confidence(),
                aiResponse.fallbackUsed()
        ));
        agentRunRepository.save(new AgentRun(query, targetAgent.name(), status));
        queryResponseRepository.save(new QueryResponse(
                query,
                aiResponse.answer(),
                buildResponseMetadata(aiResponse),
                aiResponse.sources() == null ? 0 : aiResponse.sources().size(),
                aiResponse.confidence(),
                aiResponse.fallbackReason()
        ));
        librarySearchLogService.saveIfLibrarySearch(query, aiResponse);
        logResult(query, traceId, conversation, aiResponse, status, agentResult.latencyMs());

        return toCreateResponse(query, traceId, aiResponse);
    }

    private AgentResult callOrchestrator(Query query, OrchestrateRequest orchestrateRequest) {
        AiRequestLog aiRequestLog = aiRequestLogRepository.save(new AiRequestLog(
                query,
                orchestrateRequest.traceId(),
                AiOrchestratorClient.ORCHESTRATOR_CHAT_ENDPOINT,
                toMap(orchestrateRequest)
        ));
        Instant startedAt = Instant.now();
        try {
            OrchestrateResponse aiResponse = aiGatewayService.orchestrateChat(orchestrateRequest);
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            aiResponseLogRepository.save(new AiResponseLog(aiRequestLog, 200, toMap(aiResponse), latencyMs));
            log.info(
                    "core_orchestrator_chat queryUid={} traceId={} targetAgent={} intent={} confidence={} fallbackUsed={} fallbackReason={} statusCode={} timeout=false latencyMs={}",
                    query.getQueryUid(),
                    orchestrateRequest.traceId(),
                    aiResponse.targetAgent(),
                    aiResponse.intent(),
                    aiResponse.confidence(),
                    aiResponse.fallbackUsed(),
                    aiResponse.fallbackReason(),
                    200,
                    latencyMs
            );
            return new AgentResult(aiResponse, latencyMs);
        } catch (RuntimeException exception) {
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            int statusCode = statusCode(exception);
            aiResponseLogRepository.save(new AiResponseLog(
                    aiRequestLog,
                    statusCode,
                    Map.of("error", exception.getClass().getSimpleName(), "message", safeMessage(exception)),
                    latencyMs
            ));
            log.warn(
                    "core_orchestrator_chat queryUid={} traceId={} targetAgent={} statusCode={} timeout={} latencyMs={} error={}",
                    query.getQueryUid(),
                    orchestrateRequest.traceId(),
                    TargetAgent.FALLBACK.name(),
                    statusCode,
                    isTimeout(exception),
                    latencyMs,
                    exception.getClass().getSimpleName()
            );
            return new AgentResult(null, latencyMs);
        }
    }

    private void logResult(
            Query query,
            UUID traceId,
            Conversation conversation,
            OrchestrateResponse response,
            String status,
            long latencyMs
    ) {
        log.info(
                "core_orchestrator queryUid={} traceId={} conversationUid={} message={} targetAgent={} intent={} confidence={} status={} latencyMs={} fallbackUsed={} fallbackReason={}",
                query.getQueryUid(),
                traceId,
                conversation.getConversationUid(),
                query.getQueryText(),
                response.targetAgent(),
                response.intent(),
                response.confidence(),
                status,
                latencyMs,
                response.fallbackUsed(),
                response.fallbackReason()
        );
    }

    private QueryCreateResponse toCreateResponse(Query query, UUID traceId, OrchestrateResponse aiResponse) {
        return new QueryCreateResponse(
                query.getQueryUid(),
                traceId,
                aiResponse.targetAgent(),
                aiResponse.intent(),
                aiResponse.answer(),
                aiResponse.sources(),
                aiResponse.confidence(),
                aiResponse.fallbackUsed(),
                aiResponse.fallbackReason(),
                aiResponse.searchKeyword(),
                aiResponse.resultCount(),
                aiResponse.matchedBooks(),
                aiResponse.requiresDocumentInput()
        );
    }

    private Map<String, Object> buildResponseMetadata(OrchestrateResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sources", response.sources() == null ? List.of() : response.sources());
        if (isLibrarySearchResponse(response)) {
            metadata.put("library", buildLibrarySearchMetadata(response));
        }
        return metadata;
    }

    private boolean isLibrarySearchResponse(OrchestrateResponse response) {
        return response.searchKeyword() != null
                || response.resultCount() != null
                || (response.matchedBooks() != null && !response.matchedBooks().isEmpty());
    }

    private Map<String, Object> buildLibrarySearchMetadata(OrchestrateResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfNotNull(metadata, "searchKeyword", response.searchKeyword());
        putIfNotNull(metadata, "resultCount", response.resultCount());
        metadata.put("matchedBooks", response.matchedBooks() == null ? List.of() : response.matchedBooks());
        return metadata;
    }

    private void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "" : exception.getMessage();
    }

    private int statusCode(RuntimeException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value();
        }
        return 500;
    }

    private boolean isTimeout(RuntimeException exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        Throwable cause = exception.getCause();
        while (cause != null) {
            String className = cause.getClass().getName().toLowerCase();
            if (className.contains("timeout") || className.contains("timedout")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }

    private record AgentResult(OrchestrateResponse response, long latencyMs) {
    }
}
