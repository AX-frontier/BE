package com.axprontier.api.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrchestrateResponse(
        String targetAgent,
        String intent,
        String answer,
        List<SourceDto> sources,
        BigDecimal confidence,
        boolean fallbackUsed,
        String fallbackReason
) {
}
