package com.axprontier.api.query.dto;

import com.axprontier.api.ai.dto.MatchedBookDto;
import com.axprontier.api.ai.dto.SourceDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QueryCreateResponse(
        UUID queryUid,
        UUID traceId,
        String targetAgent,
        String intent,
        String answer,
        List<SourceDto> sources,
        BigDecimal confidence,
        boolean fallbackUsed,
        String fallbackReason,
        String searchKeyword,
        Integer resultCount,
        List<MatchedBookDto> matchedBooks,
        boolean requiresDocumentInput
) {
}
