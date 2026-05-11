package com.axprontier.api.query.dto;

import com.axprontier.api.ai.dto.MatchedBookDto;
import com.axprontier.api.ai.dto.SourceDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CoreQueryResponse(
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
        Map<String, Object> summary,
        List<Map<String, Object>> findings,
        List<Map<String, Object>> criterionResults,
        List<Map<String, Object>> checkRequiredItems,
        List<Map<String, Object>> formatNoticeItems,
        List<Map<String, Object>> extractedTables,
        Map<String, Object> revisedDocument,
        String reviewMarkdown
) {
}
