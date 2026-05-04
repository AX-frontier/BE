package com.axprontier.api.library.service;

import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.library.entity.BookSearchLog;
import com.axprontier.api.library.repository.BookSearchLogRepository;
import com.axprontier.api.query.entity.Query;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LibrarySearchLogService {

    private static final String LIBRARY_TARGET_AGENT = "LIBRARY";

    private final BookSearchLogRepository bookSearchLogRepository;
    private final ObjectMapper objectMapper;

    public LibrarySearchLogService(BookSearchLogRepository bookSearchLogRepository, ObjectMapper objectMapper) {
        this.bookSearchLogRepository = bookSearchLogRepository;
        this.objectMapper = objectMapper;
    }

    public void saveIfLibrarySearch(Query query, OrchestrateResponse response) {
        if (!isLibraryResponse(response) || !hasSearchMetadata(response)) {
            return;
        }

        Map<String, Object> resultsJson = new LinkedHashMap<>();
        resultsJson.put("matchedBooks", response.matchedBooks() == null ? List.of() : toMapList(response.matchedBooks()));
        resultsJson.put("sources", response.sources() == null ? List.of() : toMapList(response.sources()));
        resultsJson.put("intent", response.intent());
        resultsJson.put("confidence", response.confidence());
        resultsJson.put("fallbackUsed", response.fallbackUsed());

        bookSearchLogRepository.save(new BookSearchLog(
                query,
                response.searchKeyword(),
                response.resultCount() == null ? 0 : response.resultCount(),
                resultsJson
        ));
    }

    private boolean isLibraryResponse(OrchestrateResponse response) {
        return response != null
                && response.targetAgent() != null
                && LIBRARY_TARGET_AGENT.equalsIgnoreCase(response.targetAgent());
    }

    private boolean hasSearchMetadata(OrchestrateResponse response) {
        return response.searchKeyword() != null
                || response.resultCount() != null
                || (response.matchedBooks() != null && !response.matchedBooks().isEmpty());
    }

    private List<Map<String, Object>> toMapList(Object value) {
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }
}
