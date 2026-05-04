package com.axprontier.api.conversation.repository;

import com.axprontier.api.conversation.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByConversationUid(UUID conversationUid);
}
