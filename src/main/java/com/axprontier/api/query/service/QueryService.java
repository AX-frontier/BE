package com.axprontier.api.query.service;

import com.axprontier.api.ai.client.AiOrchestratorClient;
import com.axprontier.api.ai.entity.AiRequestLog;
import com.axprontier.api.ai.entity.AiResponseLog;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.RouteRequest;
import com.axprontier.api.ai.dto.RouteResponse;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                null
        );
        RouteRequest routeRequest = new RouteRequest(
                query.getQueryUid(),
                traceId,
                conversation.getConversationUid(),
                query.getQueryText()
        );
        RouteResult routeResult = route(query, routeRequest);
        if (routeResult.response() == null) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse("FALLBACK", "ROUTE_FAILED");
            saveFallbackResult(query, traceId, conversation, fallbackResponse, routeResult.latencyMs());
            return toCreateResponse(query, traceId, fallbackResponse);
        }

        RouteResponse routeResponse = routeResult.response();
        TargetAgent targetAgent = TargetAgent.from(routeResponse.targetAgent());
        queryRouteRepository.save(new QueryRoute(
                query,
                routeResponse.intent(),
                targetAgent.name(),
                routeResponse.confidence(),
                targetAgent == TargetAgent.FALLBACK
        ));

        if (targetAgent == TargetAgent.FALLBACK) {
            OrchestrateResponse fallbackResponse = aiGatewayService.fallbackResponse(routeResponse.intent(), "ROUTE_TARGET_FALLBACK");
            agentRunRepository.save(new AgentRun(query, TargetAgent.FALLBACK.name(), "COMPLETED"));
            queryResponseRepository.save(new QueryResponse(
                    query,
                    fallbackResponse.answer(),
                    Map.of("sources", List.of()),
                    0,
                    fallbackResponse.confidence(),
                    fallbackResponse.fallbackReason()
            ));
            logResult(query, traceId, conversation, routeResponse, fallbackResponse, "COMPLETED", routeResult.latencyMs());
            return toCreateResponse(query, traceId, fallbackResponse);
        }

        AgentResult agentResult = callAgent(query, targetAgent, orchestrateRequest);
        OrchestrateResponse aiResponse = agentResult.response();
        String status = "COMPLETED";
        if (aiResponse == null) {
            status = "FAILED";
            aiResponse = aiGatewayService.fallbackResponse(routeResponse.intent(), "AGENT_CALL_FAILED");
        }

        agentRunRepository.save(new AgentRun(query, targetAgent.name(), status));
        queryResponseRepository.save(new QueryResponse(
                query,
                aiResponse.answer(),
                Map.of("sources", aiResponse.sources() == null ? List.of() : aiResponse.sources()),
                aiResponse.sources() == null ? 0 : aiResponse.sources().size(),
                aiResponse.confidence(),
                aiResponse.fallbackReason()
        ));
        librarySearchLogService.saveIfLibrarySearch(query, aiResponse);
        logResult(query, traceId, conversation, routeResponse, aiResponse, status, routeResult.latencyMs() + agentResult.latencyMs());

        return toCreateResponse(query, traceId, aiResponse);
    }

    private RouteResult route(Query query, RouteRequest routeRequest) {
        AiRequestLog routeLog = aiRequestLogRepository.save(new AiRequestLog(
                query,
                routeRequest.traceId(),
                AiOrchestratorClient.ROUTE_ENDPOINT,
                toMap(routeRequest)
        ));
        Instant startedAt = Instant.now();
        try {
            RouteResponse routeResponse = aiGatewayService.route(routeRequest);
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            aiResponseLogRepository.save(new AiResponseLog(routeLog, 200, toMap(routeResponse), latencyMs));
            return new RouteResult(routeResponse, latencyMs);
        } catch (RuntimeException exception) {
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            aiResponseLogRepository.save(new AiResponseLog(
                    routeLog,
                    500,
                    Map.of("error", exception.getClass().getSimpleName(), "message", safeMessage(exception)),
                    latencyMs
            ));
            return new RouteResult(null, latencyMs);
        }
    }

    private AgentResult callAgent(Query query, TargetAgent targetAgent, OrchestrateRequest orchestrateRequest) {
        String endpoint = aiGatewayService.endpointFor(targetAgent);

        AiRequestLog aiRequestLog = aiRequestLogRepository.save(new AiRequestLog(
                query,
                orchestrateRequest.traceId(),
                endpoint,
                toMap(orchestrateRequest)
        ));

        Instant startedAt = Instant.now();
        try {
            OrchestrateResponse aiResponse = aiGatewayService.chat(targetAgent, orchestrateRequest);
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            aiResponseLogRepository.save(new AiResponseLog(aiRequestLog, 200, toMap(aiResponse), latencyMs));
            return new AgentResult(aiResponse, latencyMs);
        } catch (RuntimeException exception) {
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            aiResponseLogRepository.save(new AiResponseLog(
                    aiRequestLog,
                    500,
                    Map.of("error", exception.getClass().getSimpleName(), "message", safeMessage(exception)),
                    latencyMs
            ));
            return new AgentResult(null, latencyMs);
        }
    }

    private void saveFallbackResult(
            Query query,
            UUID traceId,
            Conversation conversation,
            OrchestrateResponse fallbackResponse,
            long latencyMs
    ) {
        queryRouteRepository.save(new QueryRoute(
                query,
                fallbackResponse.intent(),
                fallbackResponse.targetAgent(),
                fallbackResponse.confidence(),
                true
        ));
        agentRunRepository.save(new AgentRun(query, TargetAgent.FALLBACK.name(), "FAILED"));
        queryResponseRepository.save(new QueryResponse(
                query,
                fallbackResponse.answer(),
                Map.of("sources", List.of()),
                0,
                fallbackResponse.confidence(),
                fallbackResponse.fallbackReason()
        ));
        logResult(query, traceId, conversation, null, fallbackResponse, "FAILED", latencyMs);
    }

    private void logResult(
            Query query,
            UUID traceId,
            Conversation conversation,
            RouteResponse routeResponse,
            OrchestrateResponse response,
            String status,
            long latencyMs
    ) {
        log.info(
                "core_orchestrator queryUid={} traceId={} conversationUid={} targetAgent={} intent={} status={} latencyMs={} fallbackUsed={} routeReason={}",
                query.getQueryUid(),
                traceId,
                conversation.getConversationUid(),
                response.targetAgent(),
                response.intent(),
                status,
                latencyMs,
                response.fallbackUsed(),
                routeResponse == null ? "-" : routeResponse.reason()
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
                aiResponse.matchedBooks()
        );
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "" : exception.getMessage();
    }

    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }

    private record RouteResult(RouteResponse response, long latencyMs) {
    }

    private record AgentResult(OrchestrateResponse response, long latencyMs) {
    }
}
