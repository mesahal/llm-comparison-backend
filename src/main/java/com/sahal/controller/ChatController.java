package com.sahal.controller;

import com.sahal.entity.Conversation;
import com.sahal.entity.Message;
import com.sahal.entity.MessageRole;
import com.sahal.service.ConversationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/chat")
@CrossOrigin("*")
public class ChatController {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;

    // Free models available on OpenRouter
    private static final String[] FREE_MODELS = {
        "deepseek/deepseek-chat-v3.1:free",
        "x-ai/grok-4-fast:free", 
        "google/gemma-3-27b-it:free"
    };

    public ChatController(RestTemplate restTemplate, ObjectMapper objectMapper, ConversationService conversationService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.conversationService = conversationService;
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestParam String question,
                                  @RequestParam(value = "model", defaultValue = "all") String model,
                                  @RequestParam(value = "sessionId", required = false) String sessionId) {
        try {
            // Generate session ID if not provided
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            }
            
            if ("all".equals(model.toLowerCase())) {
                return getComparisonResponse(question, sessionId);
            } else {
                String modelName = switch (model.toLowerCase()) {
                    case "deepseek" -> FREE_MODELS[0];
                    case "grok" -> FREE_MODELS[1];
                    case "gemma" -> FREE_MODELS[2];
                    default -> throw new IllegalArgumentException("Unsupported model: " + model + ". Available: deepseek, grok, gemma, all");
                };
                
                String response = getResponseWithModel(modelName, question, sessionId);
                return Map.of("model", modelName, "response", response);
            }
        } catch (Exception e) {
            return Map.of("error", "Error: " + e.getMessage() + ". Please try again or contact support.");
        }
    }

    @GetMapping("/history/{sessionId}")
    public List<Map<String, Object>> getConversationHistory(@PathVariable String sessionId,
                                                           @RequestParam(value = "includeSummary", defaultValue = "false") boolean includeSummary) {
        List<Message> messages = conversationService.getConversationHistory(sessionId);
        List<Map<String, Object>> result = messages.stream()
                .map(msg -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("role", msg.getRole().name().toLowerCase());
                    messageMap.put("content", msg.getContent());
                    messageMap.put("timestamp", msg.getCreatedAt().toString());
                    messageMap.put("model", msg.getModelName());
                    return messageMap;
                })
                .toList();
        
        // Add summary if requested
        if (includeSummary) {
            try {
                Conversation conversation = conversationService.getOrCreateConversation(sessionId);
                if (conversation.getSummary() != null) {
                    Map<String, Object> summaryMap = new HashMap<>();
                    summaryMap.put("role", "system");
                    summaryMap.put("content", "Past conversation summary: " + conversation.getSummary());
                    summaryMap.put("timestamp", "summary");
                    summaryMap.put("model", "system");
                    result.add(0, summaryMap); // Add at the beginning
                }
            } catch (Exception e) {
                // Ignore errors when adding summary
            }
        }
        
        return result;
    }

    @DeleteMapping("/history/{sessionId}")
    public String clearConversationHistory(@PathVariable String sessionId) {
        conversationService.clearConversationHistory(sessionId);
        return "Conversation history cleared for session: " + sessionId;
    }

    @GetMapping("/conversations")
    public List<Map<String, Object>> getAllConversations() {
        return conversationService.getAllConversations();
    }

    @GetMapping("/conversations/{sessionId}")
    public Map<String, Object> getConversationDetails(@PathVariable String sessionId) {
        return conversationService.getConversationDetails(sessionId);
    }

    @DeleteMapping("/conversations/{sessionId}")
    public String deleteConversation(@PathVariable String sessionId) {
        conversationService.deleteConversation(sessionId);
        return "Conversation deleted: " + sessionId;
    }


    private Map<String, Object> getComparisonResponse(String question, String sessionId) {
        try {
            // Get or create conversation
            Conversation conversation = conversationService.getOrCreateConversation(sessionId);
            
            // Add user message to conversation
            conversationService.addMessage(conversation, MessageRole.USER, question, null);
            
            // Get conversation history for context (comparison mode - user messages only)
            List<Map<String, String>> contextMessages = conversationService.getConversationContextForComparison(sessionId);
            
            Map<String, Object> responses = new HashMap<>();
            List<Map<String, Object>> modelResponses = new ArrayList<>();
            
            // Call all models in parallel
            for (String model : FREE_MODELS) {
                try {
                    String response = callModel(model, contextMessages);
                    responses.put(model, response);
                    modelResponses.add(Map.of("model", model, "response", response, "status", "success"));
                    
                    // Add AI response to conversation
                    conversationService.addMessage(conversation, MessageRole.ASSISTANT, response, model);
                } catch (Exception e) {
                    String errorMsg = "Error: " + e.getMessage();
                    responses.put(model, errorMsg);
                    modelResponses.add(Map.of("model", model, "response", errorMsg, "status", "error"));
                }
            }
            
            return Map.of(
                "question", question,
                "sessionId", sessionId,
                "responses", responses,
                "modelResponses", modelResponses,
                "timestamp", LocalDateTime.now().toString()
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get comparison response: " + e.getMessage(), e);
        }
    }
    
    private String getResponseWithModel(String model, String question, String sessionId) {
        try {
            // Get or create conversation
            Conversation conversation = conversationService.getOrCreateConversation(sessionId);
            
            // Add user message to conversation
            conversationService.addMessage(conversation, MessageRole.USER, question, null);
            
            // Get conversation history for context (single model mode - with summarization)
            List<Map<String, String>> messages = conversationService.getConversationContextForSingleModel(sessionId, question);
            
            String response = callModel(model, messages);
            
            // Add AI response to conversation
            conversationService.addMessage(conversation, MessageRole.ASSISTANT, response, model);
            
            return response;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get response from model " + model + ": " + e.getMessage(), e);
        }
    }
    
    private String callModel(String model, List<Map<String, String>> messages) throws Exception {
        // Create the request payload
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.7);
        requestBody.put("stream", false);

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "http://localhost:8080");
        headers.set("X-Title", "AI Demo");

        // Create the request entity
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Make the API call
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/chat/completions", 
            requestEntity, 
            String.class
        );

        // Parse the response
        JsonNode jsonResponse = objectMapper.readTree(response.getBody());
        JsonNode choices = jsonResponse.get("choices");
        
        String aiResponse = "No response content found from model " + model;
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            if (message != null && message.has("content")) {
                aiResponse = message.get("content").asText();
            }
        }
        
        return aiResponse;
    }
}
