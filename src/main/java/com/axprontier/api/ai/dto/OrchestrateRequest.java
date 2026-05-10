package com.axprontier.api.ai.dto;

import java.util.UUID;
import java.util.Map;

public record OrchestrateRequest(
        UUID queryUid,
        UUID traceId,
        UUID conversationUid,
        String message,
        Map<String, Object> document
) {
    public OrchestrateRequest(UUID queryUid, UUID traceId, UUID conversationUid, String message) {
        this(queryUid, traceId, conversationUid, message, null);
    }
}
