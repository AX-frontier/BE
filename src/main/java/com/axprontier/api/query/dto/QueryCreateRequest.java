package com.axprontier.api.query.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record QueryCreateRequest(
        @NotBlank String message,
        String channel,
        Map<String, Object> document
) {
    public QueryCreateRequest(String message, String channel) {
        this(message, channel, null);
    }
}
