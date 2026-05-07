package com.axprontier.api.ai.dto;

public record SourceDto(
        Long id,
        String title,
        String sourceUrl,
        String updatedAt
) {
}
