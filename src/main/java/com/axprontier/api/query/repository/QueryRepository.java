package com.axprontier.api.query.repository;

import com.axprontier.api.query.entity.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryRepository extends JpaRepository<Query, Long> {

    Optional<Query> findByQueryUid(UUID queryUid);

    List<Query> findByConversationConversationUidOrderByCreatedAtAsc(UUID conversationUid);
}
