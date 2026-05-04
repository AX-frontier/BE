package com.axprontier.api.conversation.service;

import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.dto.ConversationCreateRequest;
import com.axprontier.api.conversation.dto.ConversationResponse;
import com.axprontier.api.conversation.repository.ConversationRepository;
import com.axprontier.api.global.error.BusinessException;
import com.axprontier.api.global.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public ConversationResponse create(ConversationCreateRequest request) {
        Conversation conversation = conversationRepository.save(new Conversation(request.title()));
        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public Conversation getByUid(UUID conversationUid) {
        return conversationRepository.findByConversationUid(conversationUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Conversation not found."));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getConversationUid(),
                conversation.getTitle(),
                conversation.getCreatedAt()
        );
    }
}
