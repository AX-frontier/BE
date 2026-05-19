package com.axprontier.api.ai.dto;

import java.util.UUID;
import java.util.Map;

public record OrchestrateRequest(
        UUID queryUid,
        UUID traceId,
        UUID conversationUid,
        String message,
        Map<String, Object> document,
        Map<String, Object> clientLocation
) {
    public OrchestrateRequest(UUID queryUid, UUID traceId, UUID conversationUid, String message) {
        this(queryUid, traceId, conversationUid, message, null, null);
    }

    public OrchestrateRequest(UUID queryUid, UUID traceId, UUID conversationUid, String message, Map<String, Object> document) {
        this(queryUid, traceId, conversationUid, message, document, null);
    }
}
