package com.axprontier.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrchestrateResponse(
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
        @JsonAlias("table_checks")
        List<TableCheckDto> tableChecks,
        @JsonAlias("table_checks_available")
        Boolean tableChecksAvailable,
        Map<String, Object> revisedDocument,
        String reviewMarkdown,
        boolean requiresDocumentInput
) {
    public OrchestrateResponse(
            String targetAgent,
            String intent,
            String answer,
            List<SourceDto> sources,
            BigDecimal confidence,
            boolean fallbackUsed,
            String fallbackReason,
            String searchKeyword,
            Integer resultCount,
            List<MatchedBookDto> matchedBooks
    ) {
        this(
                targetAgent,
                intent,
                answer,
                sources,
                confidence,
                fallbackUsed,
                fallbackReason,
                searchKeyword,
                resultCount,
                matchedBooks,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }
}
