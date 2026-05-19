package com.axprontier.api.ai.dto;

import java.math.BigDecimal;

public record RouteEvidence(
        BigDecimal mainScore,
        BigDecimal libraryScore,
        BigDecimal documentReviewScore,
        BigDecimal campusMapScore,
        String mainReason,
        String libraryReason,
        String documentReviewReason,
        String campusMapReason
) {
}
