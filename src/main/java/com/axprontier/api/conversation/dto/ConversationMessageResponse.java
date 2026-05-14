package com.axprontier.api.conversation.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ConversationMessageResponse(
        String role,
        UUID queryUid,
        String content,
        LocalDateTime createdAt,
        Map<String, Object> metadata
) {
}
