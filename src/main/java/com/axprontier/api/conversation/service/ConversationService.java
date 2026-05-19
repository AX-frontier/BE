package com.axprontier.api.conversation.service;

import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.dto.ConversationCreateRequest;
import com.axprontier.api.conversation.dto.ConversationDetailResponse;
import com.axprontier.api.conversation.dto.ConversationListItemResponse;
import com.axprontier.api.conversation.dto.ConversationMessageResponse;
import com.axprontier.api.conversation.dto.ConversationResponse;
import com.axprontier.api.conversation.repository.ConversationRepository;
import com.axprontier.api.global.apiPayload.code.GeneralErrorCode;
import com.axprontier.api.global.apiPayload.exception.GeneralException;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.entity.QueryResponse;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.query.repository.QueryResponseRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final QueryRepository queryRepository;
    private final QueryResponseRepository queryResponseRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            QueryRepository queryRepository,
            QueryResponseRepository queryResponseRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.queryRepository = queryRepository;
        this.queryResponseRepository = queryResponseRepository;
    }

    @Transactional
    public ConversationResponse create(ConversationCreateRequest request) {
        Conversation conversation = conversationRepository.save(new Conversation(request.title(), normalizeUserId(request.userId())));
        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationListItemResponse> list(String userId, int page, int size) {
        return conversationRepository.findByUserIdOrderByLastMessageAtDescUpdatedAtDesc(
                        normalizeUserId(userId),
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))
                )
                .stream()
                .map(conversation -> new ConversationListItemResponse(
                        conversation.getConversationUid(),
                        conversation.getTitle(),
                        conversation.getLastMessagePreview(),
                        conversation.getMessageCount(),
                        conversation.getLastMessageAt() == null ? conversation.getUpdatedAt() : conversation.getLastMessageAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetail(UUID conversationUid, String userId) {
        Conversation conversation = conversationRepository.findByConversationUidAndUserId(conversationUid, normalizeUserId(userId))
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND, "Conversation not found."));
        List<Query> queries = queryRepository.findByConversationConversationUidOrderByCreatedAtAsc(conversationUid);
        Map<Long, QueryResponse> responseByQuery = queryResponseRepository.findByQueryIn(queries)
                .stream()
                .collect(Collectors.toMap(response -> response.getQuery().getId(), response -> response));

        List<ConversationMessageResponse> messages = new ArrayList<>();
        for (Query query : queries) {
            messages.add(new ConversationMessageResponse(
                    "user",
                    query.getQueryUid(),
                    query.getQueryText(),
                    query.getCreatedAt(),
                    Map.of("channel", query.getChannel() == null ? "" : query.getChannel())
            ));
            QueryResponse response = responseByQuery.get(query.getId());
            if (response != null) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                if (response.getSourcesJson() == null || response.getSourcesJson().isEmpty()) {
                    metadata.put("sources", List.of());
                } else {
                    metadata.putAll(response.getSourcesJson());
                    metadata.putIfAbsent("sources", List.of());
                }
                metadata.put("sourceCount", response.getSourceCount());
                metadata.put("confidence", response.getConfidence());
                metadata.put("fallbackReason", response.getFallbackReason());
                messages.add(new ConversationMessageResponse(
                        "assistant",
                        query.getQueryUid(),
                        response.getAnswerText(),
                        response.getCreatedAt(),
                        metadata
                ));
            }
        }
        messages.sort(Comparator.comparing(ConversationMessageResponse::createdAt));
        return new ConversationDetailResponse(
                conversation.getConversationUid(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                messages
        );
    }

    @Transactional(readOnly = true)
    public Conversation getByUid(UUID conversationUid) {
        return conversationRepository.findByConversationUid(conversationUid)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND, "Conversation not found."));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getConversationUid(),
                conversation.getTitle(),
                conversation.getUserId(),
                conversation.getCreatedAt()
        );
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "anonymous";
        }
        return userId.trim();
    }
}
