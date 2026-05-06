package com.axprontier.api.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CoreQueryRequest(
        @NotNull UUID queryUid,
        @NotNull UUID traceId,
        @NotNull UUID conversationUid,
        String userId,
        @NotBlank String message
) {
}
