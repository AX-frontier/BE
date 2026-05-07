package com.axprontier.api.ai.dto;

import java.util.UUID;

public record RouteRequest(
        UUID queryUid,
        UUID traceId,
        UUID conversationUid,
        String message
) {
    public OrchestrateRequest toOrchestrateRequest() {
        return new OrchestrateRequest(queryUid, traceId, conversationUid, message);
    }
}
