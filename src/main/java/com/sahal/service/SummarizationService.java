package com.sahal.service;

import com.sahal.entity.Message;
import com.sahal.entity.MessageRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SummarizationService {
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    
    // Use a smaller, cheaper model for summarization
    private static final String SUMMARIZATION_MODEL = "deepseek/deepseek-chat-v3.1:free";
    private static final int MAX_SUMMARY_LENGTH = 100;
    private static final int SUMMARIZATION_THRESHOLD = 5; // Summarize after 5 messages
    private static final int RECENT_MESSAGES_KEEP = 3; // Keep last 3 messages after summarization
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public SummarizationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Check if conversation needs summarization based on message count
     */
    public boolean shouldSummarize(List<Message> messages) {
        return messages.size() > SUMMARIZATION_THRESHOLD;
    }
    
    /**
     * Summarize conversation history and return summary + recent messages
     */
    public SummarizationResult summarizeConversation(List<Message> messages) {
        if (messages.isEmpty()) {
            return new SummarizationResult("", messages);
        }
        
        try {
            // Split messages into old (to summarize) and recent (to keep)
            List<Message> messagesToSummarize = messages.subList(0, Math.max(0, messages.size() - RECENT_MESSAGES_KEEP));
            List<Message> recentMessages = messages.subList(Math.max(0, messages.size() - RECENT_MESSAGES_KEEP), messages.size());
            
            String summary = generateSummary(messagesToSummarize);
            
            return new SummarizationResult(summary, recentMessages);
            
        } catch (Exception e) {
            // Fallback: create a simple summary from user messages
            String fallbackSummary = createFallbackSummary(messages);
            return new SummarizationResult(fallbackSummary, messages);
        }
    }
    
    /**
     * Generate AI-powered summary using OpenRouter
     */
    private String generateSummary(List<Message> messages) throws Exception {
        if (messages.isEmpty()) {
            return "";
        }
        
        // Build conversation text for summarization
        String conversationText = messages.stream()
                .map(msg -> msg.getRole().name() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));
        
        // Create summarization prompt
        String prompt = String.format(
            "Please provide a concise summary (max %d words) of this conversation. " +
            "Focus on the main topics discussed and key points. " +
            "Do not include specific details, just the general themes:\n\n%s",
            MAX_SUMMARY_LENGTH,
            conversationText
        );
        
        // Prepare API request
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", SUMMARIZATION_MODEL);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 150);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "AI Demo - Summarization");
        
        // Make API call
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/chat/completions",
            requestEntity,
            String.class
        );
        
        // Parse response
        JsonNode jsonResponse = objectMapper.readTree(response.getBody());
        JsonNode choices = jsonResponse.get("choices");
        
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message != null && message.has("content")) {
                return message.get("content").asText().trim();
            }
        }
        
        throw new RuntimeException("Failed to get summary from API");
    }
    
    /**
     * Create fallback summary when AI summarization fails
     */
    private String createFallbackSummary(List<Message> messages) {
        List<String> userMessages = messages.stream()
                .filter(msg -> msg.getRole() == MessageRole.USER)
                .map(Message::getContent)
                .collect(Collectors.toList());
        
        if (userMessages.isEmpty()) {
            return "User has had a conversation with the AI assistant.";
        }
        
        String topics = userMessages.stream()
                .map(msg -> msg.length() > 30 ? msg.substring(0, 30) + "..." : msg)
                .collect(Collectors.joining(", "));
        
        return "User has discussed: " + topics;
    }
    
    /**
     * Build context for LLM with summary and recent messages
     */
    public List<Map<String, String>> buildContextWithSummary(String summary, List<Message> recentMessages, String currentUserMessage) {
        List<Map<String, String>> context = new ArrayList<>();
        
        // Add system message with summary if available
        if (summary != null && !summary.trim().isEmpty()) {
            context.add(Map.of(
                "role", "system",
                "content", "Past conversation summary: " + summary + ". Do not repeat this in your response, just use it as memory."
            ));
        }
        
        // Add recent conversation history
        for (Message msg : recentMessages) {
            context.add(Map.of(
                "role", msg.getRole().name().toLowerCase(),
                "content", msg.getContent()
            ));
        }
        
        // Add current user message
        context.add(Map.of(
            "role", "user",
            "content", currentUserMessage
        ));
        
        return context;
    }
    
    /**
     * Result class for summarization
     */
    public static class SummarizationResult {
        private final String summary;
        private final List<Message> recentMessages;
        
        public SummarizationResult(String summary, List<Message> recentMessages) {
            this.summary = summary;
            this.recentMessages = recentMessages;
        }
        
        public String getSummary() { return summary; }
        public List<Message> getRecentMessages() { return recentMessages; }
    }
}
