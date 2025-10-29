package com.sahal.service;

import com.sahal.entity.Message;
import com.sahal.entity.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummarizationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SummarizationService summarizationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(summarizationService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(summarizationService, "baseUrl", "https://test.openrouter.ai/api/v1");
    }

    @Test
    void shouldSummarize_WithFewMessages_ReturnsFalse() {
        // Given
        List<Message> messages = createTestMessages(3);

        // When
        boolean result = summarizationService.shouldSummarize(messages);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldSummarize_WithManyMessages_ReturnsTrue() {
        // Given
        List<Message> messages = createTestMessages(6);

        // When
        boolean result = summarizationService.shouldSummarize(messages);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldSummarize_WithEmptyList_ReturnsFalse() {
        // Given
        List<Message> messages = List.of();

        // When
        boolean result = summarizationService.shouldSummarize(messages);

        // Then
        assertFalse(result);
    }

    @Test
    void summarizeConversation_WithFewMessages_ReturnsAllMessages() {
        // Given
        List<Message> messages = createTestMessages(3);

        // When
        SummarizationService.SummarizationResult result = summarizationService.summarizeConversation(messages);

        // Then
        assertNotNull(result);
        assertEquals("", result.getSummary());
        assertEquals(messages, result.getRecentMessages());
    }

    @Test
    void summarizeConversation_WithManyMessages_ReturnsSummaryAndRecentMessages() throws Exception {
        // Given
        List<Message> messages = createTestMessages(8);
        
        // Force API failure to test fallback behavior
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("API Error"));

        // When
        SummarizationService.SummarizationResult result = summarizationService.summarizeConversation(messages);

        // Then
        assertNotNull(result);
        assertFalse(result.getSummary().isEmpty());
        assertTrue(result.getSummary().startsWith("User has discussed:"));
        assertEquals(8, result.getRecentMessages().size()); // Fallback returns all messages
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void summarizeConversation_ApiFailure_UsesFallbackSummary() throws Exception {
        // Given
        List<Message> messages = createTestMessages(8);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("API Error"));

        // When
        SummarizationService.SummarizationResult result = summarizationService.summarizeConversation(messages);

        // Then
        assertNotNull(result);
        assertTrue(result.getSummary().startsWith("User has discussed:"));
        assertEquals(8, result.getRecentMessages().size()); // Fallback returns all messages
    }

    @Test
    void buildContextWithSummary_WithSummaryAndMessages_ReturnsCorrectContext() {
        // Given
        String summary = "User discussed AI and programming topics.";
        List<Message> recentMessages = createTestMessages(2);
        String currentUserMessage = "What is machine learning?";

        // When
        List<Map<String, String>> context = summarizationService.buildContextWithSummary(
            summary, recentMessages, currentUserMessage
        );

        // Then
        assertNotNull(context);
        assertEquals(4, context.size()); // system + 2 recent + current user
        
        // Check system message
        Map<String, String> systemMessage = context.get(0);
        assertEquals("system", systemMessage.get("role"));
        assertTrue(systemMessage.get("content").contains("Past conversation summary"));
        assertTrue(systemMessage.get("content").contains(summary));
        
        // Check recent messages
        Map<String, String> firstRecent = context.get(1);
        assertEquals("user", firstRecent.get("role"));
        assertEquals("Test message 0", firstRecent.get("content"));
        
        // Check current user message
        Map<String, String> currentMessage = context.get(3);
        assertEquals("user", currentMessage.get("role"));
        assertEquals(currentUserMessage, currentMessage.get("content"));
    }

    @Test
    void buildContextWithSummary_WithoutSummary_ReturnsContextWithoutSystemMessage() {
        // Given
        String summary = null;
        List<Message> recentMessages = createTestMessages(1);
        String currentUserMessage = "Hello";

        // When
        List<Map<String, String>> context = summarizationService.buildContextWithSummary(
            summary, recentMessages, currentUserMessage
        );

        // Then
        assertNotNull(context);
        assertEquals(2, context.size()); // 1 recent + current user (no system message)
        
        // Check that first message is not a system message
        Map<String, String> firstMessage = context.get(0);
        assertNotEquals("system", firstMessage.get("role"));
    }

    @Test
    void buildContextWithSummary_WithEmptySummary_ReturnsContextWithoutSystemMessage() {
        // Given
        String summary = "";
        List<Message> recentMessages = createTestMessages(1);
        String currentUserMessage = "Hello";

        // When
        List<Map<String, String>> context = summarizationService.buildContextWithSummary(
            summary, recentMessages, currentUserMessage
        );

        // Then
        assertNotNull(context);
        assertEquals(2, context.size()); // 1 recent + current user (no system message)
    }

    private List<Message> createTestMessages(int count) {
        return List.of(
            new Message(MessageRole.USER, "Test message 0"),
            new Message(MessageRole.ASSISTANT, "Test response 0", "test-model"),
            new Message(MessageRole.USER, "Test message 1"),
            new Message(MessageRole.ASSISTANT, "Test response 1", "test-model"),
            new Message(MessageRole.USER, "Test message 2"),
            new Message(MessageRole.ASSISTANT, "Test response 2", "test-model"),
            new Message(MessageRole.USER, "Test message 3"),
            new Message(MessageRole.ASSISTANT, "Test response 3", "test-model")
        ).subList(0, count);
    }
}
