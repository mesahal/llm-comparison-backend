package com.sahal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = "session_id"))
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();
    
    // Constructors
    public Conversation() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Conversation(String sessionId) {
        this();
        this.sessionId = sessionId;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    
    public void addMessage(Message message) {
        messages.add(message);
        message.setConversation(this);
    }
}
