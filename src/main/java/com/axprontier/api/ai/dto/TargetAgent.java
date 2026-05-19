package com.axprontier.api.ai.dto;

public enum TargetAgent {
    DOCUMENT_REVIEW,
    LIBRARY,
    CAMPUS_MAP,
    MAIN,
    FALLBACK;

    public static TargetAgent from(String value) {
        if (value == null || value.isBlank()) {
            return FALLBACK;
        }
        try {
            return TargetAgent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return FALLBACK;
        }
    }
}
