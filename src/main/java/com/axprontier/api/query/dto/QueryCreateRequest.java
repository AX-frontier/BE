package com.axprontier.api.query.dto;

import jakarta.validation.constraints.NotBlank;

public record QueryCreateRequest(
        @NotBlank String message,
        String channel
) {
}
