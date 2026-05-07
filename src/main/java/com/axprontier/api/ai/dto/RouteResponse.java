package com.axprontier.api.ai.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RouteResponse(
        UUID queryUid,
        UUID traceId,
        UUID conversationUid,
        String targetAgent,
        String intent,
        BigDecimal confidence,
        String reason,
        RouteEvidence evidence
) {
}
