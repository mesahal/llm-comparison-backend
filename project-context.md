# LLM Comparison Backend - Project Context

## 🎯 Project Overview

**Name:** LLM Comparison Backend  
**Type:** Spring Boot REST API  
**Purpose:** Multi-LLM chat comparison platform with conversation persistence  
**Tech Stack:** Java 21, Spring Boot 3.5.3, PostgreSQL, JPA/Hibernate, OpenRouter API  

## 🏗️ Architecture Overview

### Core Components
- **ChatController**: Main REST API controller handling chat requests
- **ConversationService**: Business logic for conversation management with summarization
- **SummarizationService**: AI-powered conversation summarization service
- **ConversationRepository**: Data access layer for conversations
- **MessageRepository**: Data access layer for messages
- **ChatClientConfiguration**: HTTP client configuration for external APIs

### External Dependencies
- **OpenRouter API**: Unified gateway for multiple LLM models
- **PostgreSQL**: Primary database for conversation persistence
- **Spring AI**: AI integration framework

## 📊 Database Schema

### Tables

#### `conversations`
```sql
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    summary TEXT
);
```

#### `messages`
```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    model_name VARCHAR(255),
    conversation_id BIGINT REFERENCES conversations(id)
);
```

### Relationships
- **One-to-Many**: Conversation → Messages
- **Foreign Key**: messages.conversation_id → conversations.id
- **Unique Constraint**: conversations.session_id

## 🔌 API Endpoints

### 1. Chat API
**POST** `/chat/ask`

**Parameters:**
- `question` (required): User's message
- `model` (optional): `deepseek`, `grok`, `gemma`, `all` (default: `all`)
- `sessionId` (optional): Conversation session ID

**Response Types:**
- **Single Model**: `{"model": "model_name", "response": "content"}`
- **Comparison Mode**: `{"question": "...", "sessionId": "...", "responses": {...}, "modelResponses": [...], "timestamp": "..."}`

### 2. Conversation History
**GET** `/chat/history/{sessionId}?includeSummary=false`

**Parameters:**
- `includeSummary` (optional): Include conversation summary in response (default: false)

**Response:** Array of message objects with role, content, timestamp, model

### 3. Clear History
**DELETE** `/chat/history/{sessionId}`

**Response:** Confirmation message

### 4. List Conversations
**GET** `/chat/conversations`

**Response:** Array of conversation summaries with metadata

### 5. Conversation Details
**GET** `/chat/conversations/{sessionId}`

**Response:** Full conversation details with all messages

### 6. Delete Conversation
**DELETE** `/chat/conversations/{sessionId}`

**Response:** Confirmation message

## 🤖 Supported LLM Models

### Free Models (via OpenRouter)
1. **DeepSeek**: `deepseek/deepseek-chat-v3.1:free`
2. **Grok**: `x-ai/grok-4-fast:free`
3. **Gemma**: `google/gemma-3-27b-it:free`

### Model Selection
- **Single Model**: Use specific model name (`deepseek`, `grok`, `gemma`)
- **Comparison Mode**: Use `all` to get responses from all models

## 🔄 Request Flow

### Single Model Flow (with Summarization)
1. **Request**: POST `/chat/ask?question=...&model=deepseek&sessionId=...`
2. **Session Management**: Get or create conversation by sessionId
3. **Message Persistence**: Save user message to database
4. **Context Building**: 
   - Check if conversation needs summarization (>5 messages)
   - If yes: Summarize old messages, keep recent 3 messages
   - Build context: `system summary + recent messages + current user message`
   - If no: Use all conversation history
5. **API Call**: Call OpenRouter with optimized conversation context
6. **Response Persistence**: Save AI response with model attribution
7. **Return**: Single model response

### Comparison Mode Flow (No Summarization)
1. **Request**: POST `/chat/ask?question=...&model=all&sessionId=...`
2. **Session Management**: Get or create conversation by sessionId
3. **Message Persistence**: Save user message to database
4. **Context Building**: Retrieve conversation history (USER MESSAGES ONLY)
5. **Parallel API Calls**: Call all 3 models simultaneously with user-only context
6. **Response Persistence**: Save each AI response with model attribution
7. **Return**: Comparison response with all model outputs

**Note**: Comparison mode does not use summarization to maintain fairness between models.

## 🧠 Conversation Summarization

### Overview
The backend now includes intelligent conversation summarization to handle long conversations without repetition or excessive token costs.

### Summarization Triggers
- **Message Count**: Summarizes after 5+ messages in a conversation
- **Context Optimization**: Keeps only the last 3 messages + summary for context
- **Mode-Specific**: Only applies to single model mode (not comparison mode)

### Summarization Process
1. **Detection**: Check if conversation has >5 messages
2. **AI Summarization**: Call OpenRouter with conversation history
3. **Fallback**: If AI fails, create simple summary from user messages
4. **Database Update**: Save summary to `conversations.summary` column
5. **Cleanup**: Delete old messages, keep recent 3 messages
6. **Context Building**: Build context with `system summary + recent messages + current user message`

### Summarization Service
- **Model**: Uses `deepseek/deepseek-chat-v3.1:free` for cost efficiency
- **Max Summary Length**: 100 words
- **System Prompt**: "Past conversation summary: [summary]. Do not repeat this in your response, just use it as memory."
- **Error Handling**: Graceful fallback to simple text concatenation

### API Integration
- **History Endpoint**: Optional `?includeSummary=true` parameter
- **Transparent Operation**: Summarization is invisible to frontend by default
- **Backward Compatibility**: Existing APIs work unchanged

## 🗃️ Data Models

### Conversation Entity
```java
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id @GeneratedValue
    private Long id;
    
    @Column(unique = true)
    private String sessionId;
    
    private LocalDateTime createdAt;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<Message> messages;
}
```

### Message Entity
```java
@Entity
@Table(name = "messages")
public class Message {
    @Id @GeneratedValue
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private MessageRole role; // USER, ASSISTANT, SYSTEM
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private LocalDateTime createdAt;
    private String modelName;
    
    @ManyToOne
    private Conversation conversation;
}
```

### MessageRole Enum
```java
public enum MessageRole {
    USER, ASSISTANT, SYSTEM
}
```

## ⚙️ Configuration

### Application Properties
```yaml
spring:
  application:
    name: AIDemo
  ai:
    openai:
      api-key: sk-or-v1-...
      base-url: https://openrouter.ai/api/v1
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: root
    password: Admin@123
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### External API Configuration
- **Base URL**: `https://openrouter.ai/api/v1`
- **Endpoint**: `/chat/completions`
- **Authentication**: Bearer token
- **Headers**: Content-Type, Authorization, HTTP-Referer, X-Title

## 🔧 Business Logic

### Conversation Management
- **Session ID Generation**: `session_{timestamp}_{uuid}`
- **Conversation Creation**: Auto-create on first message
- **Duplicate Handling**: Retry logic with progressive backoff

### Context Building Logic

#### Single Model Mode
```java
// Retrieve ALL conversation history (user + assistant messages)
List<Message> history = conversationService.getConversationHistory(conversation.getId());

// Build complete conversation context
List<Map<String, String>> messages = new ArrayList<>();
for (Message msg : history) {
    messages.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
}
```

**Context Sent to AI**: Complete conversation history including:
- All previous user messages
- All previous assistant responses
- Current user message

#### Comparison Mode
```java
// Retrieve conversation history
List<Message> history = conversationService.getConversationHistory(conversation.getId());

// Build context with USER MESSAGES ONLY
List<Map<String, String>> contextMessages = new ArrayList<>();
for (Message msg : history) {
    if (msg.getRole() == MessageRole.USER) {
        contextMessages.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
    }
}
```

**Context Sent to AI**: Only user messages including:
- All previous user messages (excluding AI responses)
- Current user message

#### Why Different Context Strategies?

**Single Model Mode**: 
- Maintains full conversation context
- AI can reference its own previous responses
- Enables follow-up questions and corrections
- More natural conversation flow

**Comparison Mode**:
- Prevents AI models from seeing each other's responses
- Ensures fair comparison without bias
- Each model gets identical context
- Focuses on individual model capabilities

#### Context Building Flow Diagram

```
Database Conversation History:
┌─────────────────────────────────────────────────────────────┐
│ User: "What is AI?"                                         │
│ Assistant: "AI is artificial intelligence..." (DeepSeek)   │
│ User: "How does it work?"                                   │
│ Assistant: "AI works by..." (DeepSeek)                     │
│ User: "Give me examples"                                    │
└─────────────────────────────────────────────────────────────┘

Single Model Mode Context:
┌─────────────────────────────────────────────────────────────┐
│ [{"role": "user", "content": "What is AI?"},               │
│  {"role": "assistant", "content": "AI is artificial..."},  │
│  {"role": "user", "content": "How does it work?"},         │
│  {"role": "assistant", "content": "AI works by..."},       │
│  {"role": "user", "content": "Give me examples"}]          │
└─────────────────────────────────────────────────────────────┘

Comparison Mode Context (User Messages Only):
┌─────────────────────────────────────────────────────────────┐
│ [{"role": "user", "content": "What is AI?"},               │
│  {"role": "user", "content": "How does it work?"},         │
│  {"role": "user", "content": "Give me examples"}]          │
└─────────────────────────────────────────────────────────────┘
```

### Message Persistence
- **User Messages**: Saved with `MessageRole.USER`, no model attribution
- **AI Responses**: Saved with `MessageRole.ASSISTANT` and model name
- **Chronological Ordering**: Messages ordered by `createdAt` timestamp

### Error Handling
- **API Failures**: Individual model failures don't break entire request
- **Database Conflicts**: Retry logic for unique constraint violations
- **External API Errors**: Graceful degradation with error messages

## 🚨 Known Limitations & Issues

### Scalability Issues
- **No Connection Pooling**: Default HikariCP settings
- **N+1 Query Problem**: Lazy loading triggers multiple queries
- **Memory Leaks**: Unbounded conversation history
- **No Caching**: Repeated database queries

### Security Issues
- **API Key Exposure**: Hardcoded in application.yml
- **No Authentication**: Open access to all endpoints
- **CORS Wildcard**: Allows any origin
- **No Input Validation**: Potential SQL injection/XSS

### Production Readiness
- **No Rate Limiting**: Will hit external API limits
- **No Circuit Breaker**: Cascading failures
- **No Monitoring**: No logs, metrics, or health checks
- **Thread Blocking**: Synchronous external API calls

## 🔄 Integration Points

### Frontend Integration
- **Base URL**: `http://localhost:8080`
- **CORS**: Enabled for all origins
- **Content-Type**: `application/json`
- **Session Management**: Frontend manages sessionId persistence

### External API Integration
- **OpenRouter**: Primary LLM provider
- **Request Format**: OpenAI-compatible chat completions
- **Response Parsing**: JSON extraction from choices[0].message.content
- **Error Handling**: Graceful degradation on API failures

## 📝 Development Notes

### Key Files
- `ChatController.java`: Main API controller
- `ConversationService.java`: Business logic layer
- `Conversation.java`: Conversation entity
- `Message.java`: Message entity
- `application.yml`: Configuration
- `pom.xml`: Maven dependencies

### Dependencies
- Spring Boot Web Starter
- Spring AI OpenAI Starter
- Spring Boot Data JPA
- PostgreSQL Driver
- Jackson (JSON processing)

### Build & Run
- **Build**: `mvn clean install`
- **Run**: `mvn spring-boot:run`
- **Port**: 8080 (default)
- **Database**: PostgreSQL on localhost:5432

## 🎯 Use Cases

### Primary Use Cases
1. **Multi-Model Comparison**: Compare responses from different LLMs
2. **Conversation Persistence**: Maintain chat history across sessions
3. **Context-Aware Chat**: Send conversation history to maintain context
4. **Model-Specific Chat**: Use individual models for specific tasks

### Secondary Use Cases
1. **Conversation Management**: List, view, and delete conversations
2. **API Integration**: RESTful API for frontend applications
3. **Development Testing**: Test different LLM models and responses

This project provides a foundation for LLM comparison and conversation management, but requires significant improvements for production scalability and security.
