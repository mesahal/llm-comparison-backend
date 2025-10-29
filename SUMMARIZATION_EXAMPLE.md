# Conversation Summarization Example

## Overview
This document demonstrates how the conversation summarization feature works in the LLM Comparison Backend.

## Example Scenario

### Initial Conversation (5 messages - No Summarization)
```
User: "What is artificial intelligence?"
Assistant: "AI is the simulation of human intelligence in machines..."
User: "How does machine learning work?"
Assistant: "Machine learning is a subset of AI that enables computers to learn..."
User: "Can you give me examples of ML algorithms?"
Assistant: "Sure! Common ML algorithms include linear regression, decision trees..."
```

### After 6th Message (Summarization Triggered)
When the user sends the 6th message, the system:

1. **Detects** conversation has >5 messages
2. **Summarizes** the first 3 messages (old messages)
3. **Keeps** the last 3 messages (recent context)
4. **Builds** new context with summary + recent messages

### Context Sent to LLM
```
System: "Past conversation summary: User discussed AI basics, machine learning concepts, and requested examples of ML algorithms. Do not repeat this in your response, just use it as memory."

User: "Can you give me examples of ML algorithms?"
Assistant: "Sure! Common ML algorithms include linear regression, decision trees..."
User: "What about deep learning?"
Assistant: "Deep learning uses neural networks with multiple layers..."
User: "How do I get started with AI?"
```

## API Usage Examples

### 1. Single Model Chat (with Summarization)
```bash
# First few messages - no summarization
curl -X POST "http://localhost:8080/chat/ask?question=What is AI?&model=deepseek&sessionId=test123"

# After 6th message - summarization kicks in
curl -X POST "http://localhost:8080/chat/ask?question=How do I learn AI?&model=deepseek&sessionId=test123"
```

### 2. Comparison Mode (No Summarization)
```bash
# Comparison mode never uses summarization for fairness
curl -X POST "http://localhost:8080/chat/ask?question=What is AI?&model=all&sessionId=test123"
```

### 3. History with Summary
```bash
# Include summary in history response
curl -X GET "http://localhost:8080/chat/history/test123?includeSummary=true"
```

## Database Changes

### Before Summarization
```sql
-- conversations table
id | session_id | created_at | summary
1  | test123    | 2024-01-01 | NULL

-- messages table (6 messages)
id | role | content | conversation_id
1  | USER | What is AI? | 1
2  | ASSISTANT | AI is... | 1
3  | USER | How does ML work? | 1
4  | ASSISTANT | ML is... | 1
5  | USER | Give examples | 1
6  | ASSISTANT | Examples are... | 1
```

### After Summarization
```sql
-- conversations table (updated)
id | session_id | created_at | summary
1  | test123    | 2024-01-01 | User discussed AI basics, machine learning concepts, and requested examples of ML algorithms.

-- messages table (only recent 3 messages kept)
id | role | content | conversation_id
4  | ASSISTANT | ML is... | 1
5  | USER | Give examples | 1
6  | ASSISTANT | Examples are... | 1
```

## Configuration

### Summarization Settings
- **Trigger Threshold**: 5 messages
- **Recent Messages Kept**: 3 messages
- **Max Summary Length**: 100 words
- **Summarization Model**: `deepseek/deepseek-chat-v3.1:free`

### System Prompt
```
"Past conversation summary: [summary]. Do not repeat this in your response, just use it as memory."
```

## Benefits

1. **Token Cost Reduction**: Reduces API costs by ~60-80% for long conversations
2. **Context Preservation**: Maintains conversation context without repetition
3. **Performance**: Faster API calls with smaller context
4. **Memory Efficiency**: Reduces database storage for old messages
5. **Fair Comparison**: Comparison mode remains unaffected for model fairness

## Error Handling

### API Failure Fallback
If the summarization API fails, the system creates a simple fallback summary:
```
"User has discussed: What is AI?, How does ML work?, Give examples..."
```

### Graceful Degradation
- Summarization failures don't break the chat
- System continues with full conversation history
- Logs errors for monitoring

## Testing

Run the summarization tests:
```bash
mvn test -Dtest=SummarizationServiceTest
```

The tests cover:
- Summarization trigger logic
- Fallback behavior
- Context building
- Error handling
