package com.axprontier.api.conversation.entity;

import com.axprontier.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "last_message_preview", length = 300)
    private String lastMessagePreview;

    @Column(name = "message_count")
    private int messageCount;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    protected Conversation() {
    }

    public Conversation(String title) {
        this.conversationUid = UUID.randomUUID();
        this.title = title;
        this.userId = "anonymous";
        this.messageCount = 0;
    }

    public Conversation(UUID conversationUid, String title) {
        this.conversationUid = conversationUid;
        this.title = title;
        this.userId = "anonymous";
        this.messageCount = 0;
    }

    public Conversation(String title, String userId) {
        this.conversationUid = UUID.randomUUID();
        this.title = title;
        this.userId = userId;
        this.messageCount = 0;
    }

    public Conversation(UUID conversationUid, String title, String userId) {
        this.conversationUid = conversationUid;
        this.title = title;
        this.userId = userId;
        this.messageCount = 0;
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

    public String getUserId() {
        return userId;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void updateActivity(String messagePreview, LocalDateTime messageAt, int addedMessageCount) {
        if (title == null || title.isBlank()) {
            title = messagePreview.length() > 30 ? messagePreview.substring(0, 30) : messagePreview;
        }
        lastMessagePreview = messagePreview.length() > 300 ? messagePreview.substring(0, 300) : messagePreview;
        lastMessageAt = messageAt;
        messageCount += addedMessageCount;
    }
}
