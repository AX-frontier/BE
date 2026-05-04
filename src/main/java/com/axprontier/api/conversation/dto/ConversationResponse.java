package com.axprontier.api.conversation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationUid,
        String title,
        OffsetDateTime createdAt
) {
}
