package com.axprontier.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TableCheckDto(
        String id,
        @JsonAlias("table_index")
        Integer tableIndex,
        @JsonAlias("table_title")
        String tableTitle,
        String category,
        String severity,
        String status,
        String message,
        String suggestion,
        Map<String, Object> evidence
) {
}
