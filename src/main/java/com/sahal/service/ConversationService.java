package com.sahal.service;

import com.sahal.entity.Conversation;
import com.sahal.entity.Message;
import com.sahal.entity.MessageRole;
import com.sahal.repository.ConversationRepository;
import com.sahal.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@Transactional
public class ConversationService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private SummarizationService summarizationService;
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Conversation getOrCreateConversation(String sessionId) {
        return getOrCreateConversationWithRetry(sessionId, 3);
    }
    
    private Conversation getOrCreateConversationWithRetry(String sessionId, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // First, try to find existing conversation
                Optional<Conversation> existingConversation = conversationRepository.findBySessionId(sessionId);
                
                if (existingConversation.isPresent()) {
                    return existingConversation.get();
                }
                
                // If not found, create a new one
                Conversation newConversation = new Conversation(sessionId);
                return conversationRepository.save(newConversation);
                
            } catch (DataIntegrityViolationException e) {
                // Handle unique constraint violation - conversation was created by another thread
                if (e.getMessage().contains("duplicate key value violates unique constraint") || 
                    e.getMessage().contains("Unique index or primary key violation") || 
                    e.getMessage().contains("constraint") && e.getMessage().contains("SESSION_ID")) {
                    
                    // Try to find the conversation that was created by another thread
                    Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
                    if (conversation.isPresent()) {
                        return conversation.get();
                    }
                    
                    // If still not found and we have retries left, wait a bit and try again
                    if (attempt < maxRetries - 1) {
                        try {
                            Thread.sleep(50 + (attempt * 25)); // Progressive backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for retry", ie);
                        }
                        continue;
                    }
                }
                throw e;
            } catch (Exception e) {
                // Handle case where multiple conversations exist with same sessionId
                if (e.getMessage().contains("Query did not return a unique result")) {
                    return handleDuplicateSessions(sessionId);
                }
                throw e;
            }
        }
        
        // This should never be reached, but just in case
        throw new RuntimeException("Failed to create or find conversation after " + maxRetries + " attempts");
    }
    
    private Conversation handleDuplicateSessions(String sessionId) {
        // Find all conversations with this sessionId
        List<Conversation> duplicateConversations = conversationRepository.findAll().stream()
                .filter(conv -> sessionId.equals(conv.getSessionId()))
                .toList();
        
        if (duplicateConversations.isEmpty()) {
            // No conversations found, create new one
            Conversation newConversation = new Conversation(sessionId);
            return conversationRepository.save(newConversation);
        }
        
        // Keep the most recent conversation (highest ID)
        Conversation latestConversation = duplicateConversations.stream()
                .max((c1, c2) -> Long.compare(c1.getId(), c2.getId()))
                .orElse(duplicateConversations.get(0));
        
        // Delete the older duplicates
        duplicateConversations.stream()
                .filter(conv -> !conv.getId().equals(latestConversation.getId()))
                .forEach(conversationRepository::delete);
        
        return latestConversation;
    }
    
    public void addMessage(Conversation conversation, MessageRole role, String content) {
        Message message = new Message(role, content);
        conversation.addMessage(message);
        messageRepository.save(message);
    }
    
    public void addMessage(Conversation conversation, MessageRole role, String content, String modelName) {
        Message message = new Message(role, content, modelName);
        conversation.addMessage(message);
        messageRepository.save(message);
    }
    
    public List<Message> getConversationHistory(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
    
    public List<Message> getConversationHistory(String sessionId) {
        try {
            Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
            if (conversation.isPresent()) {
                return getConversationHistory(conversation.get().getId());
            }
            return List.of();
        } catch (Exception e) {
            // Handle case where multiple conversations exist with same sessionId
            if (e.getMessage().contains("Query did not return a unique result")) {
                Conversation latestConversation = handleDuplicateSessions(sessionId);
                return getConversationHistory(latestConversation.getId());
            }
            throw e;
        }
    }
    
    /**
     * Get conversation history with optional summarization for single model mode
     * This method handles summarization logic for long conversations
     */
    public List<Map<String, String>> getConversationContextForSingleModel(String sessionId, String currentUserMessage) {
        try {
            Optional<Conversation> conversationOpt = conversationRepository.findBySessionId(sessionId);
            if (conversationOpt.isEmpty()) {
                // No conversation exists, just return current message
                return List.of(Map.of("role", "user", "content", currentUserMessage));
            }
            
            Conversation conversation = conversationOpt.get();
            List<Message> allMessages = getConversationHistory(conversation.getId());
            
            // Check if we need to summarize
            if (summarizationService.shouldSummarize(allMessages)) {
                // Perform summarization
                SummarizationService.SummarizationResult result = summarizationService.summarizeConversation(allMessages);
                
                // Update conversation with new summary
                conversation.setSummary(result.getSummary());
                conversationRepository.save(conversation);
                
                // Delete old messages that were summarized (keep only recent ones)
                List<Message> messagesToDelete = allMessages.subList(0, allMessages.size() - result.getRecentMessages().size());
                messageRepository.deleteAll(messagesToDelete);
                
                // Build context with summary and recent messages
                return summarizationService.buildContextWithSummary(
                    result.getSummary(), 
                    result.getRecentMessages(), 
                    currentUserMessage
                );
            } else {
                // No summarization needed, build context with all messages
                List<Map<String, String>> context = new ArrayList<>();
                for (Message msg : allMessages) {
                    context.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
                }
                context.add(Map.of("role", "user", "content", currentUserMessage));
                return context;
            }
            
        } catch (Exception e) {
            // Fallback: just return current message
            return List.of(Map.of("role", "user", "content", currentUserMessage));
        }
    }
    
    /**
     * Get conversation history for comparison mode (user messages only, no summarization)
     * This maintains the existing behavior for fair model comparison
     */
    public List<Map<String, String>> getConversationContextForComparison(String sessionId) {
        try {
            Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
            if (conversation.isEmpty()) {
                return List.of();
            }
            
            List<Message> history = getConversationHistory(conversation.get().getId());
            List<Map<String, String>> contextMessages = new ArrayList<>();
            
            // Only include user messages for comparison mode
            for (Message msg : history) {
                if (msg.getRole() == MessageRole.USER) {
                    contextMessages.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
                }
            }
            
            return contextMessages;
            
        } catch (Exception e) {
            return List.of();
        }
    }
    
    public void clearConversationHistory(String sessionId) {
        try {
            Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
            if (conversation.isPresent()) {
                conversationRepository.delete(conversation.get());
            }
        } catch (Exception e) {
            // Handle case where multiple conversations exist with same sessionId
            if (e.getMessage().contains("Query did not return a unique result")) {
                // Delete all conversations with this sessionId
                List<Conversation> duplicateConversations = conversationRepository.findAll().stream()
                        .filter(conv -> sessionId.equals(conv.getSessionId()))
                        .toList();
                duplicateConversations.forEach(conversationRepository::delete);
            } else {
                throw e;
            }
        }
    }
    
    public List<Map<String, Object>> getAllConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        return conversations.stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt())) // Most recent first
                .map(this::convertToConversationSummary)
                .toList();
    }
    
    public Map<String, Object> getConversationDetails(String sessionId) {
        try {
            Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
            if (conversation.isPresent()) {
                return convertToConversationDetails(conversation.get());
            }
            return Map.of("error", "Conversation not found");
        } catch (Exception e) {
            if (e.getMessage().contains("Query did not return a unique result")) {
                // Handle duplicates by returning the most recent one
                List<Conversation> duplicates = conversationRepository.findAll().stream()
                        .filter(conv -> sessionId.equals(conv.getSessionId()))
                        .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                        .toList();
                if (!duplicates.isEmpty()) {
                    return convertToConversationDetails(duplicates.get(0));
                }
            }
            return Map.of("error", "Error retrieving conversation: " + e.getMessage());
        }
    }
    
    public void deleteConversation(String sessionId) {
        try {
            Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
            if (conversation.isPresent()) {
                conversationRepository.delete(conversation.get());
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Query did not return a unique result")) {
                // Delete all conversations with this sessionId
                List<Conversation> duplicateConversations = conversationRepository.findAll().stream()
                        .filter(conv -> sessionId.equals(conv.getSessionId()))
                        .toList();
                duplicateConversations.forEach(conversationRepository::delete);
            } else {
                throw e;
            }
        }
    }
    
    private Map<String, Object> convertToConversationSummary(Conversation conversation) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("sessionId", conversation.getSessionId());
        summary.put("createdAt", conversation.getCreatedAt().toString());
        summary.put("messageCount", conversation.getMessages().size());
        summary.put("hasSummary", conversation.getSummary() != null && !conversation.getSummary().trim().isEmpty());
        
        // Get the first user message as title (or first message if no user message)
        String title = "New Conversation";
        if (!conversation.getMessages().isEmpty()) {
            Message firstMessage = conversation.getMessages().stream()
                    .filter(msg -> msg.getRole() == MessageRole.USER)
                    .findFirst()
                    .orElse(conversation.getMessages().get(0));
            title = firstMessage.getContent().length() > 50 
                ? firstMessage.getContent().substring(0, 50) + "..." 
                : firstMessage.getContent();
        }
        summary.put("title", title);
        
        // Get last message timestamp
        if (!conversation.getMessages().isEmpty()) {
            Message lastMessage = conversation.getMessages().stream()
                    .max((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                    .orElse(null);
            if (lastMessage != null) {
                summary.put("lastMessageAt", lastMessage.getCreatedAt().toString());
            }
        }
        
        // Get unique models used in this conversation
        List<String> models = conversation.getMessages().stream()
                .filter(msg -> msg.getModelName() != null)
                .map(Message::getModelName)
                .distinct()
                .toList();
        summary.put("models", models);
        
        return summary;
    }
    
    private Map<String, Object> convertToConversationDetails(Conversation conversation) {
        Map<String, Object> details = new HashMap<>();
        details.put("sessionId", conversation.getSessionId());
        details.put("createdAt", conversation.getCreatedAt().toString());
        details.put("messageCount", conversation.getMessages().size());
        details.put("summary", conversation.getSummary());
        
        // Include all messages
        List<Map<String, Object>> messages = conversation.getMessages().stream()
                .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(msg -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("role", msg.getRole().name().toLowerCase());
                    messageMap.put("content", msg.getContent());
                    messageMap.put("timestamp", msg.getCreatedAt().toString());
                    messageMap.put("model", msg.getModelName());
                    return messageMap;
                })
                .toList();
        details.put("messages", messages);
        
        // Get unique models used in this conversation
        List<String> models = conversation.getMessages().stream()
                .filter(msg -> msg.getModelName() != null)
                .map(Message::getModelName)
                .distinct()
                .toList();
        details.put("models", models);
        
        return details;
    }
    
}
