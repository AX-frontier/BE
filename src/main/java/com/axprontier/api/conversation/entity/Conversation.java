package com.axprontier.api.conversation.entity;

import com.axprontier.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "core", name = "conversations")
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_uid", nullable = false, unique = true)
    private UUID conversationUid;

    @Column(name = "title", length = 200)
    private String title;

    protected Conversation() {
    }

    public Conversation(String title) {
        this.conversationUid = UUID.randomUUID();
        this.title = title;
    }

    public Conversation(UUID conversationUid, String title) {
        this.conversationUid = conversationUid;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public UUID getConversationUid() {
        return conversationUid;
    }

    public String getTitle() {
        return title;
    }
}
