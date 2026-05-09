package com.axprontier.api.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record DocumentReviewRequest(
        @NotNull UUID queryUid,
        @NotNull UUID traceId,
        @NotNull UUID conversationUid,
        String userId,
        @NotBlank String message,
        @NotNull Map<String, Object> document
) {
}
