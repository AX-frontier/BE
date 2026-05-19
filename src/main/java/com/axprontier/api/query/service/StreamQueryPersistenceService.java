package com.axprontier.api.query.service;

import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.TargetAgent;
import com.axprontier.api.ai.service.AiGatewayService;
import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.repository.ConversationRepository;
import com.axprontier.api.global.apiPayload.code.GeneralErrorCode;
import com.axprontier.api.global.apiPayload.exception.GeneralException;
import com.axprontier.api.library.service.LibrarySearchLogService;
import com.axprontier.api.query.dto.CoreQueryRequest;
import com.axprontier.api.query.entity.AgentRun;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.entity.QueryResponse;
import com.axprontier.api.query.entity.QueryRoute;
import com.axprontier.api.query.repository.AgentRunRepository;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.query.repository.QueryResponseRepository;
import com.axprontier.api.query.repository.QueryRouteRepository;
import com.axprontier.api.review.service.DocumentReviewService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StreamQueryPersistenceService {

    private final ConversationRepository conversationRepository;
    private final QueryRepository queryRepository;
    private final QueryRouteRepository queryRouteRepository;
    private final AgentRunRepository agentRunRepository;
    private final QueryResponseRepository queryResponseRepository;
    private final LibrarySearchLogService librarySearchLogService;
    private final DocumentReviewService documentReviewService;
    private final AiGatewayService aiGatewayService;

    public StreamQueryPersistenceService(
            ConversationRepository conversationRepository,
            QueryRepository queryRepository,
            QueryRouteRepository queryRouteRepository,
            AgentRunRepository agentRunRepository,
            QueryResponseRepository queryResponseRepository,
            LibrarySearchLogService librarySearchLogService,
            DocumentReviewService documentReviewService,
            AiGatewayService aiGatewayService
    ) {
        this.conversationRepository = conversationRepository;
        this.queryRepository = queryRepository;
        this.queryRouteRepository = queryRouteRepository;
        this.agentRunRepository = agentRunRepository;
        this.queryResponseRepository = queryResponseRepository;
        this.librarySearchLogService = librarySearchLogService;
        this.documentReviewService = documentReviewService;
        this.aiGatewayService = aiGatewayService;
    }

    @Transactional
    public void validateConversationAccess(CoreQueryRequest request) {
        String userId = normalizeUserId(request.userId());
        conversationRepository.findByConversationUid(request.conversationUid())
                .ifPresent(conversation -> assertOwner(conversation, userId));
    }

    @Transactional
    public void saveCompleted(CoreQueryRequest request, OrchestrateResponse response) {
        OrchestrateResponse terminalResponse = response == null
                ? aiGatewayService.fallbackResponse("FALLBACK", "ORCHESTRATOR_STREAM_FAILED")
                : response;
        Conversation conversation = findOrCreateConversation(request);
        Query query = queryRepository.findByQueryUid(request.queryUid())
                .orElseGet(() -> queryRepository.save(
                        new Query(request.queryUid(), conversation, request.message(), "WEB")
                ));
        if (queryResponseRepository.findByQuery(query).isPresent()) {
            return;
        }

        TargetAgent targetAgent = TargetAgent.from(terminalResponse.targetAgent());
        queryRouteRepository.save(new QueryRoute(
                query,
                terminalResponse.intent(),
                targetAgent.name(),
                terminalResponse.confidence(),
                terminalResponse.fallbackUsed()
        ));
        agentRunRepository.save(new AgentRun(
                query,
                targetAgent.name(),
                terminalResponse.fallbackUsed() || targetAgent == TargetAgent.FALLBACK ? "FAILED" : "COMPLETED"
        ));
        String answerText = terminalResponse.answer() == null ? "" : terminalResponse.answer();
        queryResponseRepository.save(new QueryResponse(
                query,
                answerText,
                buildResponseMetadata(terminalResponse),
                terminalResponse.sources() == null ? 0 : terminalResponse.sources().size(),
                terminalResponse.confidence(),
                terminalResponse.fallbackReason()
        ));
        conversation.updateActivity(
                answerText.isBlank() ? request.message() : answerText,
                LocalDateTime.now(),
                2
        );
        librarySearchLogService.saveIfLibrarySearch(query, terminalResponse);
        documentReviewService.recordStreamedReview(request, terminalResponse);
    }

    private Map<String, Object> buildResponseMetadata(OrchestrateResponse response) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sources", response.sources() == null ? List.of() : response.sources());
        if (isLibrarySearchResponse(response)) {
            metadata.put("library", buildLibrarySearchMetadata(response));
        }
        if (response.mapResult() != null) {
            metadata.put("mapResult", response.mapResult());
        }
        return metadata;
    }

    private boolean isLibrarySearchResponse(OrchestrateResponse response) {
        return TargetAgent.from(response.targetAgent()) == TargetAgent.LIBRARY
                && (response.searchKeyword() != null
                || response.resultCount() != null
                || (response.matchedBooks() != null && !response.matchedBooks().isEmpty()));
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

    private Conversation findOrCreateConversation(CoreQueryRequest request) {
        String userId = normalizeUserId(request.userId());
        return conversationRepository.findByConversationUid(request.conversationUid())
                .map(conversation -> {
                    assertOwner(conversation, userId);
                    return conversation;
                })
                .orElseGet(() -> conversationRepository.save(
                        new Conversation(request.conversationUid(), request.message(), userId)
                ));
    }

    private void assertOwner(Conversation conversation, String userId) {
        if (!userId.equals(normalizeUserId(conversation.getUserId()))) {
            throw new GeneralException(GeneralErrorCode.FORBIDDEN, "Conversation owner mismatch.");
        }
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "anonymous";
        }
        return userId.trim();
    }
}
