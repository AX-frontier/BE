package com.axprontier.api.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.conversation.repository.ConversationRepository;
import com.axprontier.api.query.entity.Query;
import com.axprontier.api.query.entity.QueryResponse;
import com.axprontier.api.query.repository.QueryRepository;
import com.axprontier.api.query.repository.QueryResponseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class ConversationServiceTest {

    private final ConversationRepository conversationRepository = org.mockito.Mockito.mock(ConversationRepository.class);
    private final QueryRepository queryRepository = org.mockito.Mockito.mock(QueryRepository.class);
    private final QueryResponseRepository queryResponseRepository = org.mockito.Mockito.mock(QueryResponseRepository.class);
    private final ConversationService service = new ConversationService(
            conversationRepository,
            queryRepository,
            queryResponseRepository
    );

    @Test
    void listsConversationsForSidebar() {
        Conversation conversation = new Conversation(UUID.randomUUID(), "학사 질문", "local-fe-user");
        conversation.updateActivity("복수전공 답변", LocalDateTime.of(2026, 5, 14, 10, 0), 2);
        when(conversationRepository.findByUserIdOrderByLastMessageAtDescUpdatedAtDesc(
                org.mockito.Mockito.eq("local-fe-user"),
                any(Pageable.class)
        )).thenReturn(List.of(conversation));

        var result = service.list("local-fe-user", 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("학사 질문");
        assertThat(result.get(0).lastMessagePreview()).isEqualTo("복수전공 답변");
        assertThat(result.get(0).messageCount()).isEqualTo(2);
    }

    @Test
    void restoresConversationMessagesFromQueriesAndResponses() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "학사 질문", "local-fe-user");
        Query query = new Query(UUID.randomUUID(), conversation, "복수전공 신청 기간 알려줘", "WEB");
        setEntityMetadata(conversation, 1L, LocalDateTime.of(2026, 5, 14, 9, 0));
        setEntityMetadata(query, 10L, LocalDateTime.of(2026, 5, 14, 9, 1));
        QueryResponse response = new QueryResponse(
                query,
                "복수전공 신청 기간 답변입니다.",
                Map.of("sources", List.of()),
                0,
                BigDecimal.valueOf(0.9),
                null
        );
        setEntityMetadata(response, 20L, LocalDateTime.of(2026, 5, 14, 9, 2));

        when(conversationRepository.findByConversationUidAndUserId(conversationUid, "local-fe-user"))
                .thenReturn(Optional.of(conversation));
        when(queryRepository.findByConversationConversationUidOrderByCreatedAtAsc(conversationUid))
                .thenReturn(List.of(query));
        when(queryResponseRepository.findByQueryIn(List.of(query)))
                .thenReturn(List.of(response));

        var result = service.getDetail(conversationUid, "local-fe-user");

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().get(0).role()).isEqualTo("user");
        assertThat(result.messages().get(0).content()).isEqualTo("복수전공 신청 기간 알려줘");
        assertThat(result.messages().get(1).role()).isEqualTo("assistant");
        assertThat(result.messages().get(1).content()).isEqualTo("복수전공 신청 기간 답변입니다.");
        assertThat(result.messages().get(1).metadata().get("sources")).isEqualTo(List.of());
    }

    @Test
    void restoresCampusMapResultFromResponseMetadata() {
        UUID conversationUid = UUID.randomUUID();
        Conversation conversation = new Conversation(conversationUid, "위치 질문", "local-fe-user");
        Query query = new Query(UUID.randomUUID(), conversation, "상상관 위치 알려줘", "WEB");
        Map<String, Object> mapResult = Map.of("campusId", "hansung", "mode", "place");
        QueryResponse response = new QueryResponse(
                query,
                "상상관 위치 안내입니다.",
                Map.of("sources", List.of(), "mapResult", mapResult),
                0,
                BigDecimal.valueOf(0.9),
                null
        );
        setEntityMetadata(conversation, 1L, LocalDateTime.of(2026, 5, 14, 9, 0));
        setEntityMetadata(query, 10L, LocalDateTime.of(2026, 5, 14, 9, 1));
        setEntityMetadata(response, 20L, LocalDateTime.of(2026, 5, 14, 9, 2));

        when(conversationRepository.findByConversationUidAndUserId(conversationUid, "local-fe-user"))
                .thenReturn(Optional.of(conversation));
        when(queryRepository.findByConversationConversationUidOrderByCreatedAtAsc(conversationUid))
                .thenReturn(List.of(query));
        when(queryResponseRepository.findByQueryIn(List.of(query)))
                .thenReturn(List.of(response));

        var result = service.getDetail(conversationUid, "local-fe-user");

        assertThat(result.messages().get(1).metadata().get("mapResult")).isEqualTo(mapResult);
    }

    private void setEntityMetadata(Object entity, Long id, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        ReflectionTestUtils.setField(entity, "updatedAt", createdAt);
    }
}
