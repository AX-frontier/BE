package com.axprontier.api.ai.dto;

import java.util.UUID;

public record OrchestrateRequest(
        UUID queryUid,
        UUID traceId,
        UUID conversationUid,
        String message
) {
}
