package com.axprontier.api.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CoreQueryRequest(
        @NotNull UUID queryUid,
        @NotNull UUID traceId,
        @NotNull UUID conversationUid,
        String userId,
        @NotBlank String message,
        Map<String, Object> document,
        Map<String, Object> clientLocation
) {
    public CoreQueryRequest(UUID queryUid, UUID traceId, UUID conversationUid, String userId, String message) {
        this(queryUid, traceId, conversationUid, userId, message, null, null);
    }

    public CoreQueryRequest(UUID queryUid, UUID traceId, UUID conversationUid, String userId, String message, Map<String, Object> document) {
        this(queryUid, traceId, conversationUid, userId, message, document, null);
    }
}
