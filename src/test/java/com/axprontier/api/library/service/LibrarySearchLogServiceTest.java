package com.axprontier.api.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.axprontier.api.ai.dto.MatchedBookDto;
import com.axprontier.api.ai.dto.OrchestrateResponse;
import com.axprontier.api.ai.dto.SourceDto;
import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.library.entity.BookSearchLog;
import com.axprontier.api.library.repository.BookSearchLogRepository;
import com.axprontier.api.query.entity.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LibrarySearchLogServiceTest {

    private final BookSearchLogRepository bookSearchLogRepository = mock(BookSearchLogRepository.class);
    private final LibrarySearchLogService librarySearchLogService =
            new LibrarySearchLogService(bookSearchLogRepository, new ObjectMapper());

    @Test
    void savesLibrarySearchMetadata() {
        Query query = new Query(new Conversation("test conversation"), "클린 코드 찾아줘", "WEB");
        OrchestrateResponse response = new OrchestrateResponse(
                "LIBRARY",
                "BOOK_SEARCH",
                "도서 1건을 찾았습니다.",
                List.of(new SourceDto(10L, "학술정보관", "https://library.example", "2026-05-01T00:00:00+00:00")),
                BigDecimal.valueOf(0.91),
                false,
                null,
                "클린 코드",
                1,
                List.of(new MatchedBookDto(
                        10L,
                        "BIB-1",
                        "REG-1",
                        "클린 코드",
                        "Robert C. Martin",
                        "인사이트",
                        2013,
                        "005.1 M381c",
                        "단행본",
                        "MAIN",
                        "중앙도서관",
                        "3층"
                ))
        );

        librarySearchLogService.saveIfLibrarySearch(query, response);

        ArgumentCaptor<BookSearchLog> captor = ArgumentCaptor.forClass(BookSearchLog.class);
        verify(bookSearchLogRepository).save(captor.capture());

        BookSearchLog savedLog = captor.getValue();
        assertThat(savedLog.getQuery()).isSameAs(query);
        assertThat(savedLog.getKeyword()).isEqualTo("클린 코드");
        assertThat(savedLog.getResultCount()).isEqualTo(1);
        assertThat(savedLog.getResultsJson())
                .containsEntry("intent", "BOOK_SEARCH")
                .containsEntry("fallbackUsed", false);
        assertThat(savedLog.getResultsJson().get("matchedBooks")).asList().hasSize(1);
    }

    @Test
    void skipsLibraryResponseWithoutSearchMetadata() {
        Query query = new Query(new Conversation("test conversation"), "학술정보관 운영시간 알려줘", "WEB");
        OrchestrateResponse response = new OrchestrateResponse(
                "LIBRARY",
                "LIBRARY_GUIDE",
                "운영시간 안내입니다.",
                List.of(),
                BigDecimal.valueOf(0.8),
                false,
                null,
                null,
                null,
                null
        );

        librarySearchLogService.saveIfLibrarySearch(query, response);

        verify(bookSearchLogRepository, never()).save(any());
    }

    @Test
    void skipsNonLibraryResponse() {
        Query query = new Query(new Conversation("test conversation"), "문서 검토해줘", "WEB");
        OrchestrateResponse response = new OrchestrateResponse(
                "REVIEW",
                "DOCUMENT_REVIEW",
                "문서 검토 결과입니다.",
                List.of(),
                BigDecimal.valueOf(0.8),
                false,
                null,
                "문서",
                2,
                List.of()
        );

        librarySearchLogService.saveIfLibrarySearch(query, response);

        verify(bookSearchLogRepository, never()).save(any());
    }
}
