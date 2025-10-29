package com.sahal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageRole role;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "model_name")
    private String modelName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    
    // Constructors
    public Message() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Message(MessageRole role, String content) {
        this();
        this.role = role;
        this.content = content;
    }
    
    public Message(MessageRole role, String content, String modelName) {
        this();
        this.role = role;
        this.content = content;
        this.modelName = modelName;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
}
