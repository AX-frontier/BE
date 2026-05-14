package com.axprontier.api.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationListItemResponse(
        UUID conversationUid,
        String title,
        String lastMessagePreview,
        int messageCount,
        LocalDateTime updatedAt
) {
}
