package com.axprontier.api.conversation.repository;

import com.axprontier.api.conversation.entity.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByConversationUid(UUID conversationUid);

    Optional<Conversation> findByConversationUidAndUserId(UUID conversationUid, String userId);

    List<Conversation> findByUserIdOrderByLastMessageAtDescUpdatedAtDesc(String userId, Pageable pageable);
}
