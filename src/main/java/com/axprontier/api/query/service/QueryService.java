package com.axprontier.api.query.service;

import com.axprontier.api.ai.entity.AiRequestLog;
import com.axprontier.api.ai.entity.AiResponseLog;
import com.axprontier.api.ai.dto.OrchestrateRequest;
import com.axprontier.api.ai.dto.OrchestrateResponse;
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
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryService {

    private static final String ORCHESTRATE_ENDPOINT = "/ai/orchestrate";

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
                query.getQueryText()
        );

        AiRequestLog aiRequestLog = aiRequestLogRepository.save(new AiRequestLog(
                query,
                traceId,
                ORCHESTRATE_ENDPOINT,
                toMap(orchestrateRequest)
        ));

        Instant startedAt = Instant.now();
        OrchestrateResponse aiResponse = aiGatewayService.orchestrate(orchestrateRequest);
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();

        aiResponseLogRepository.save(new AiResponseLog(aiRequestLog, 200, toMap(aiResponse), latencyMs));
        queryRouteRepository.save(new QueryRoute(
                query,
                aiResponse.intent(),
                aiResponse.targetAgent(),
                aiResponse.confidence(),
                aiResponse.fallbackUsed()
        ));
        agentRunRepository.save(new AgentRun(query, aiResponse.targetAgent(), "COMPLETED"));
        queryResponseRepository.save(new QueryResponse(
                query,
                aiResponse.answer(),
                Map.of("sources", aiResponse.sources() == null ? java.util.List.of() : aiResponse.sources()),
                aiResponse.sources() == null ? 0 : aiResponse.sources().size(),
                aiResponse.confidence(),
                aiResponse.fallbackReason()
        ));
        librarySearchLogService.saveIfLibrarySearch(query, aiResponse);

        return new QueryCreateResponse(
                query.getQueryUid(),
                traceId,
                aiResponse.targetAgent(),
                aiResponse.intent(),
                aiResponse.answer(),
                aiResponse.sources(),
                aiResponse.confidence(),
                aiResponse.fallbackUsed()
        );
    }

    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }
}
