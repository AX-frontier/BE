package com.axprontier.api.conversation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationDetailResponse(
        UUID conversationUid,
        String title,
        LocalDateTime createdAt,
        List<ConversationMessageResponse> messages
) {
}
