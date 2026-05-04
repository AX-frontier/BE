package com.axprontier.api.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationUid,
        String title,
        LocalDateTime createdAt
) {
}
