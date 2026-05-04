package com.axprontier.api.query.entity;

import com.axprontier.api.conversation.entity.Conversation;
import com.axprontier.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "core", name = "queries")
public class Query extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_uid", nullable = false, unique = true)
    private UUID queryUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "query_text", nullable = false, columnDefinition = "text")
    private String queryText;

    @Column(name = "channel", length = 30)
    private String channel;

    protected Query() {
    }

    public Query(Conversation conversation, String queryText, String channel) {
        this.queryUid = UUID.randomUUID();
        this.conversation = conversation;
        this.queryText = queryText;
        this.channel = channel;
    }

    public Long getId() {
        return id;
    }

    public UUID getQueryUid() {
        return queryUid;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public String getQueryText() {
        return queryText;
    }

    public String getChannel() {
        return channel;
    }
}
