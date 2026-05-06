package com.axprontier.api.query.dto;

import com.axprontier.api.ai.dto.SourceDto;
import java.math.BigDecimal;
import java.util.List;

public record CoreQueryResponse(
        String targetAgent,
        String intent,
        String answer,
        List<SourceDto> sources,
        BigDecimal confidence,
        boolean fallbackUsed,
        String fallbackReason
) {
}
