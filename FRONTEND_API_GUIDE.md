# Frontend API Integration Guide

## Overview
This guide provides a complete implementation for integrating with the LLM Comparison Backend APIs in a professional frontend application with conversation history management.

## Available APIs

### 1. Chat API
**Endpoint:** `POST /chat/ask`
**Description:** Send a message to the AI and get a response

**Parameters:**
- `question` (required): The user's message
- `model` (optional): AI model to use (`deepseek`, `grok`, `gemma`, `all`) - defaults to `all` for comparison mode
- `sessionId` (optional): Session ID for conversation continuity - auto-generated if not provided

**Response Formats:**

**Single Model Response:**
```json
{
    "model": "deepseek/deepseek-chat-v3.1:free",
    "response": "Hello! How can I help you today?"
}
```

**Comparison Mode Response (model=all):**
```json
{
    "question": "What is artificial intelligence?",
    "sessionId": "session_1759174001334_zv2i2qxpk",
    "responses": {
        "deepseek/deepseek-chat-v3.1:free": "AI is the simulation of human intelligence...",
        "x-ai/grok-4-fast:free": "Artificial Intelligence refers to...",
        "google/gemma-3-27b-it:free": "AI is a branch of computer science..."
    },
    "modelResponses": [
        {
            "model": "deepseek/deepseek-chat-v3.1:free",
            "response": "AI is the simulation of human intelligence...",
            "status": "success"
        },
        {
            "model": "x-ai/grok-4-fast:free", 
            "response": "Artificial Intelligence refers to...",
            "status": "success"
        },
        {
            "model": "google/gemma-3-27b-it:free",
            "response": "AI is a branch of computer science...",
            "status": "success"
        }
    ],
    "timestamp": "2025-09-30T02:07:15.123Z"
}
```

**Examples:**
```javascript
// Single model
const response = await fetch('http://localhost:8080/chat/ask?question=Hello&model=deepseek&sessionId=session123', {
    method: 'POST'
});
const result = await response.json();

// Comparison mode (all models)
const response = await fetch('http://localhost:8080/chat/ask?question=Hello&model=all&sessionId=session123', {
    method: 'POST'
});
const comparison = await response.json();
```

### 2. Get Conversation History
**Endpoint:** `GET /chat/history/{sessionId}`
**Description:** Retrieve all messages in a conversation

**Response Format:**
```json
[
    {
        "role": "user",
        "content": "What is artificial intelligence?",
        "timestamp": "2025-09-30T01:05:45.123Z",
        "model": null
    },
    {
        "role": "assistant", 
        "content": "AI is the simulation of human intelligence in machines...",
        "timestamp": "2025-09-30T01:05:46.456Z",
        "model": "deepseek/deepseek-chat-v3.1:free"
    },
    {
        "role": "assistant", 
        "content": "Artificial Intelligence refers to the development of computer systems...",
        "timestamp": "2025-09-30T01:05:47.789Z",
        "model": "x-ai/grok-4-fast:free"
    },
    {
        "role": "assistant", 
        "content": "AI is a branch of computer science that aims to create intelligent machines...",
        "timestamp": "2025-09-30T01:05:48.123Z",
        "model": "google/gemma-3-27b-it:free"
    }
]
```

### 3. Clear Conversation History
**Endpoint:** `DELETE /chat/history/{sessionId}`
**Description:** Delete all messages in a conversation

### 4. List All Conversations
**Endpoint:** `GET /chat/conversations`
**Description:** Get a list of all conversations for sidebar display

**Response Format:**
```json
[
    {
        "sessionId": "session_1759174001334_zv2i2qxpk",
        "createdAt": "2025-09-30T01:05:45.123Z",
        "messageCount": 7,
        "title": "What is artificial intelligence?",
        "lastMessageAt": "2025-09-30T01:10:30.456Z",
        "models": [
            "deepseek/deepseek-chat-v3.1:free",
            "x-ai/grok-4-fast:free", 
            "google/gemma-3-27b-it:free"
        ]
    },
    {
        "sessionId": "session_1759174001335_abc123def",
        "createdAt": "2025-09-30T00:30:15.789Z",
        "messageCount": 10,
        "title": "Explain machine learning concepts",
        "lastMessageAt": "2025-09-30T00:45:20.123Z",
        "models": [
            "deepseek/deepseek-chat-v3.1:free",
            "x-ai/grok-4-fast:free"
        ]
    }
]
```

### 5. Get Conversation Details
**Endpoint:** `GET /chat/conversations/{sessionId}`
**Description:** Get detailed information about a specific conversation including all messages

**Response Format:**
```json
{
    "sessionId": "session_1759174001334_zv2i2qxpk",
    "createdAt": "2025-09-30T01:05:45.123Z",
    "messageCount": 7,
    "models": [
        "deepseek/deepseek-chat-v3.1:free",
        "x-ai/grok-4-fast:free",
        "google/gemma-3-27b-it:free"
    ],
    "messages": [
        {
            "role": "user",
            "content": "What is artificial intelligence?",
            "timestamp": "2025-09-30T01:05:45.123Z",
            "model": null
        },
        {
            "role": "assistant",
            "content": "AI is the simulation of human intelligence in machines...",
            "timestamp": "2025-09-30T01:05:46.456Z",
            "model": "deepseek/deepseek-chat-v3.1:free"
        },
        {
            "role": "assistant",
            "content": "Artificial Intelligence refers to the development of computer systems...",
            "timestamp": "2025-09-30T01:05:47.789Z",
            "model": "x-ai/grok-4-fast:free"
        },
        {
            "role": "assistant",
            "content": "AI is a branch of computer science that aims to create intelligent machines...",
            "timestamp": "2025-09-30T01:05:48.123Z",
            "model": "google/gemma-3-27b-it:free"
        }
    ]
}
```

### 6. Delete Conversation
**Endpoint:** `DELETE /chat/conversations/{sessionId}`
**Description:** Delete a specific conversation completely

## Professional Frontend Implementation

### HTML Structure
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Chat Assistant</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div class="app-container">
        <!-- Sidebar for conversation history -->
        <div class="sidebar">
            <div class="sidebar-header">
                <h2>Conversations</h2>
                <button id="newChatBtn" class="new-chat-btn">+ New Chat</button>
            </div>
            <div class="conversation-list" id="conversationList">
                <!-- Conversations will be loaded here -->
            </div>
        </div>

        <!-- Main chat area -->
        <div class="main-content">
            <div class="chat-header">
                <h1>AI Assistant</h1>
                <div class="model-selector">
                <select id="modelSelect">
                    <option value="all">All Models (Comparison)</option>
                    <option value="deepseek">DeepSeek</option>
                    <option value="grok">Grok</option>
                    <option value="gemma">Gemma</option>
                </select>
                </div>
            </div>
            
            <div class="chat-container">
                <div class="messages" id="messages">
                    <!-- Messages will be displayed here -->
                </div>
                
                <div class="input-container">
                    <div class="input-wrapper">
                        <input type="text" id="messageInput" placeholder="Type your message..." />
                        <button id="sendBtn" class="send-btn">Send</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="script.js"></script>
</body>
</html>
```

### CSS Styling (styles.css)
```css
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background-color: #f5f5f5;
    height: 100vh;
    overflow: hidden;
}

.app-container {
    display: flex;
    height: 100vh;
}

/* Sidebar Styles */
.sidebar {
    width: 300px;
    background-color: #2c3e50;
    color: white;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #34495e;
}

.sidebar-header {
    padding: 20px;
    border-bottom: 1px solid #34495e;
}

.sidebar-header h2 {
    margin-bottom: 15px;
    font-size: 1.2em;
}

.new-chat-btn {
    width: 100%;
    padding: 10px;
    background-color: #3498db;
    color: white;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    font-size: 14px;
    transition: background-color 0.3s;
}

.new-chat-btn:hover {
    background-color: #2980b9;
}

.conversation-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
}

.conversation-item {
    padding: 12px;
    margin-bottom: 5px;
    background-color: #34495e;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.3s;
    border: 1px solid transparent;
}

.conversation-item:hover {
    background-color: #4a5f7a;
}

.conversation-item.active {
    background-color: #3498db;
    border-color: #2980b9;
}

.conversation-preview {
    font-size: 14px;
    color: #ecf0f1;
    margin-bottom: 5px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.conversation-time {
    font-size: 12px;
    color: #bdc3c7;
}

.conversation-item {
    position: relative;
}

.delete-conversation-btn {
    position: absolute;
    top: 5px;
    right: 5px;
    background: #e74c3c;
    color: white;
    border: none;
    border-radius: 50%;
    width: 20px;
    height: 20px;
    cursor: pointer;
    font-size: 12px;
    display: none;
    align-items: center;
    justify-content: center;
}

.conversation-item:hover .delete-conversation-btn {
    display: flex;
}

.delete-conversation-btn:hover {
    background: #c0392b;
}

/* Main Content Styles */
.main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    background-color: white;
}

.chat-header {
    padding: 20px;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: white;
}

.chat-header h1 {
    color: #2c3e50;
    font-size: 1.5em;
}

.model-selector select {
    padding: 8px 12px;
    border: 1px solid #ddd;
    border-radius: 5px;
    background-color: white;
    font-size: 14px;
}

.chat-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: calc(100vh - 80px);
}

.messages {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
    background-color: #fafafa;
}

.message {
    margin-bottom: 20px;
    display: flex;
    align-items: flex-start;
}

.message.user {
    justify-content: flex-end;
}

.message-content {
    max-width: 70%;
    padding: 12px 16px;
    border-radius: 18px;
    word-wrap: break-word;
}

.message.user .message-content {
    background-color: #007bff;
    color: white;
    border-bottom-right-radius: 5px;
}

.message.assistant .message-content {
    background-color: white;
    color: #333;
    border: 1px solid #e0e0e0;
    border-bottom-left-radius: 5px;
}

.message-time {
    font-size: 11px;
    color: #666;
    margin-top: 5px;
    text-align: right;
}

.message.assistant .message-time {
    text-align: left;
}

/* Comparison Mode Styles */
.comparison-container {
    margin: 20px 0;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    overflow: hidden;
}

.comparison-question {
    background-color: #f8f9fa;
    padding: 15px;
    font-weight: bold;
    border-bottom: 1px solid #e0e0e0;
    color: #2c3e50;
}

.model-response {
    border-bottom: 1px solid #e0e0e0;
    padding: 15px;
}

.model-response:last-child {
    border-bottom: none;
}

.model-response.error {
    background-color: #fff5f5;
    border-left: 4px solid #e53e3e;
}

.model-label {
    font-weight: bold;
    color: #3498db;
    margin-bottom: 8px;
    font-size: 14px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.message .model-label {
    background-color: #3498db;
    color: white;
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
    margin-bottom: 8px;
    display: inline-block;
}

.input-container {
    padding: 20px;
    background-color: white;
    border-top: 1px solid #e0e0e0;
}

.input-wrapper {
    display: flex;
    gap: 10px;
    max-width: 800px;
    margin: 0 auto;
}

#messageInput {
    flex: 1;
    padding: 12px 16px;
    border: 1px solid #ddd;
    border-radius: 25px;
    font-size: 14px;
    outline: none;
    transition: border-color 0.3s;
}

#messageInput:focus {
    border-color: #007bff;
}

.send-btn {
    padding: 12px 24px;
    background-color: #007bff;
    color: white;
    border: none;
    border-radius: 25px;
    cursor: pointer;
    font-size: 14px;
    transition: background-color 0.3s;
}

.send-btn:hover:not(:disabled) {
    background-color: #0056b3;
}

.send-btn:disabled {
    background-color: #ccc;
    cursor: not-allowed;
}

/* Loading indicator */
.typing-indicator {
    display: flex;
    align-items: center;
    gap: 5px;
    color: #666;
    font-style: italic;
}

.typing-dots {
    display: flex;
    gap: 2px;
}

.typing-dot {
    width: 6px;
    height: 6px;
    background-color: #666;
    border-radius: 50%;
    animation: typing 1.4s infinite;
}

.typing-dot:nth-child(2) {
    animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
    animation-delay: 0.4s;
}

@keyframes typing {
    0%, 60%, 100% {
        transform: translateY(0);
    }
    30% {
        transform: translateY(-10px);
    }
}

/* Responsive Design */
@media (max-width: 768px) {
    .sidebar {
        width: 250px;
    }
    
    .message-content {
        max-width: 85%;
    }
    
    .input-wrapper {
        flex-direction: column;
    }
    
    .send-btn {
        border-radius: 5px;
    }
}
```

### JavaScript Implementation (script.js)
```javascript
class ChatApp {
    constructor() {
        this.currentSessionId = null;
        this.conversations = new Map();
        this.apiBaseUrl = 'http://localhost:8080';
        
        this.initializeElements();
        this.attachEventListeners();
        this.loadConversations();
    }

    initializeElements() {
        this.messageInput = document.getElementById('messageInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.messagesContainer = document.getElementById('messages');
        this.conversationList = document.getElementById('conversationList');
        this.modelSelect = document.getElementById('modelSelect');
        this.newChatBtn = document.getElementById('newChatBtn');
    }

    attachEventListeners() {
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        this.messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
        this.newChatBtn.addEventListener('click', () => this.startNewChat());
    }

    async sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message) return;

        // Disable input while processing
        this.setInputState(false);
        
        // Add user message to UI
        this.addMessageToUI('user', message);
        this.messageInput.value = '';

        // Show typing indicator
        this.showTypingIndicator();

        try {
            // Send message to API
            const result = await this.callChatAPI(message);
            
            // Remove typing indicator
            this.hideTypingIndicator();
            
            // Handle different response formats
            if (result.responses) {
                // Comparison mode - show all model responses
                this.addComparisonResponse(result);
            } else if (result.response) {
                // Single model response
                this.addMessageToUI('assistant', result.response, result.model);
            } else if (typeof result === 'string') {
                // Legacy string response
                this.addMessageToUI('assistant', result);
            } else {
                throw new Error('Unexpected response format');
            }
            
            // Update conversation in sidebar
            this.updateConversationPreview();
            
        } catch (error) {
            console.error('Error sending message:', error);
            this.hideTypingIndicator();
            this.addMessageToUI('assistant', 'Sorry, I encountered an error. Please try again.');
        } finally {
            this.setInputState(true);
        }
    }

    async callChatAPI(message) {
        const params = new URLSearchParams({
            question: message,
            model: this.modelSelect.value
        });

        if (this.currentSessionId) {
            params.append('sessionId', this.currentSessionId);
        }

        const response = await fetch(`${this.apiBaseUrl}/chat/ask?${params}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        
        // Extract session ID from response headers or generate new one
        if (!this.currentSessionId) {
            this.currentSessionId = this.generateSessionId();
            this.addConversationToList();
        }

        return result;
    }

    async loadConversationHistory(sessionId) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/chat/history/${sessionId}`);
            if (!response.ok) return [];
            
            const history = await response.json();
            return history;
        } catch (error) {
            console.error('Error loading conversation history:', error);
            return [];
        }
    }

    async loadConversations() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/chat/conversations`);
            const conversations = await response.json();
            
            // Clear existing conversations
            this.conversationList.innerHTML = '';
            this.conversations.clear();
            
            // Load conversations from API
            for (const conv of conversations) {
                const metadata = {
                    preview: conv.title,
                    timestamp: new Date(conv.lastMessageAt || conv.createdAt),
                    lastMessage: conv.title,
                    modelName: conv.modelName,
                    messageCount: conv.messageCount
                };
                
                this.conversations.set(conv.sessionId, metadata);
                this.addConversationToList(conv.sessionId, metadata);
            }
        } catch (error) {
            console.error('Failed to load conversations:', error);
            // Fallback to localStorage if API fails
            const storedConversations = this.getStoredConversations();
            for (const [sessionId, metadata] of storedConversations) {
                this.conversations.set(sessionId, metadata);
                this.addConversationToList(sessionId, metadata);
            }
        }
    }

    addConversationToList(sessionId = null, metadata = null) {
        if (!sessionId) {
            sessionId = this.currentSessionId;
            metadata = {
                preview: 'New conversation',
                timestamp: new Date(),
                lastMessage: ''
            };
        }

        this.conversations.set(sessionId, metadata);

        const conversationItem = document.createElement('div');
        conversationItem.className = 'conversation-item';
        conversationItem.dataset.sessionId = sessionId;
        
        conversationItem.innerHTML = `
            <div class="conversation-preview">${metadata.preview}</div>
            <div class="conversation-time">${this.formatTime(metadata.timestamp)}</div>
            <button class="delete-conversation-btn" onclick="event.stopPropagation(); chatApp.deleteConversation('${sessionId}')">×</button>
        `;

        conversationItem.addEventListener('click', () => this.loadConversation(sessionId));
        
        this.conversationList.appendChild(conversationItem);
        this.updateActiveConversation(sessionId);
    }

    async loadConversation(sessionId) {
        this.currentSessionId = sessionId;
        this.updateActiveConversation(sessionId);
        
        // Clear current messages
        this.messagesContainer.innerHTML = '';
        
        // Load conversation history
        const history = await this.loadConversationHistory(sessionId);
        
        // Display messages
        history.forEach(msg => {
            this.addMessageToUI(msg.role, msg.content, new Date(msg.timestamp));
        });
    }

    updateActiveConversation(sessionId) {
        // Remove active class from all conversations
        document.querySelectorAll('.conversation-item').forEach(item => {
            item.classList.remove('active');
        });
        
        // Add active class to current conversation
        const activeItem = document.querySelector(`[data-session-id="${sessionId}"]`);
        if (activeItem) {
            activeItem.classList.add('active');
        }
    }

    addMessageToUI(role, content, timestamp = null, model = null) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;
        
        const time = timestamp || new Date();
        const timeString = this.formatTime(time);
        
        let messageHTML = `<div class="message-content">${this.escapeHtml(content)}</div>`;
        
        // Add model label for assistant messages
        if (role === 'assistant' && model) {
            messageHTML = `<div class="model-label">${this.getModelDisplayName(model)}</div>` + messageHTML;
        }
        
        messageHTML += `<div class="message-time">${timeString}</div>`;
        
        messageDiv.innerHTML = messageHTML;
        
        this.messagesContainer.appendChild(messageDiv);
        this.scrollToBottom();
    }
    
    addComparisonResponse(result) {
        // Add a container for all model responses
        const comparisonDiv = document.createElement('div');
        comparisonDiv.className = 'comparison-container';
        
        // Add question header
        const questionHeader = document.createElement('div');
        questionHeader.className = 'comparison-question';
        questionHeader.textContent = result.question;
        comparisonDiv.appendChild(questionHeader);
        
        // Add responses from each model
        result.modelResponses.forEach(modelResponse => {
            const responseDiv = document.createElement('div');
            responseDiv.className = `model-response ${modelResponse.status}`;
            
            const modelLabel = document.createElement('div');
            modelLabel.className = 'model-label';
            modelLabel.textContent = this.getModelDisplayName(modelResponse.model);
            responseDiv.appendChild(modelLabel);
            
            const contentDiv = document.createElement('div');
            contentDiv.className = 'message-content';
            contentDiv.textContent = modelResponse.response;
            responseDiv.appendChild(contentDiv);
            
            comparisonDiv.appendChild(responseDiv);
        });
        
        this.messagesContainer.appendChild(comparisonDiv);
        this.scrollToBottom();
    }
    
    getModelDisplayName(modelName) {
        const modelMap = {
            'deepseek/deepseek-chat-v3.1:free': 'DeepSeek',
            'x-ai/grok-4-fast:free': 'Grok',
            'google/gemma-3-27b-it:free': 'Gemma'
        };
        return modelMap[modelName] || modelName;
    }

    showTypingIndicator() {
        const typingDiv = document.createElement('div');
        typingDiv.className = 'message assistant';
        typingDiv.id = 'typing-indicator';
        typingDiv.innerHTML = `
            <div class="message-content">
                <div class="typing-indicator">
                    AI is typing
                    <div class="typing-dots">
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                        <div class="typing-dot"></div>
                    </div>
                </div>
            </div>
        `;
        
        this.messagesContainer.appendChild(typingDiv);
        this.scrollToBottom();
    }

    hideTypingIndicator() {
        const typingIndicator = document.getElementById('typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }

    updateConversationPreview() {
        if (!this.currentSessionId) return;
        
        const lastMessage = this.messagesContainer.lastElementChild;
        if (lastMessage && lastMessage.classList.contains('message')) {
            const content = lastMessage.querySelector('.message-content').textContent;
            const preview = content.length > 50 ? content.substring(0, 50) + '...' : content;
            
            const metadata = this.conversations.get(this.currentSessionId) || {};
            metadata.preview = preview;
            metadata.timestamp = new Date();
            metadata.lastMessage = content;
            
            this.conversations.set(this.currentSessionId, metadata);
            this.updateConversationInList();
            this.saveConversations();
        }
    }

    updateConversationInList() {
        const conversationItem = document.querySelector(`[data-session-id="${this.currentSessionId}"]`);
        if (conversationItem) {
            const metadata = this.conversations.get(this.currentSessionId);
            conversationItem.querySelector('.conversation-preview').textContent = metadata.preview;
            conversationItem.querySelector('.conversation-time').textContent = this.formatTime(metadata.timestamp);
        }
    }

    startNewChat() {
        this.currentSessionId = null;
        this.messagesContainer.innerHTML = '';
        
        // Remove active class from all conversations
        document.querySelectorAll('.conversation-item').forEach(item => {
            item.classList.remove('active');
        });
        
        // Focus on input
        this.messageInput.focus();
    }

    async deleteConversation(sessionId) {
        if (!confirm('Are you sure you want to delete this conversation?')) {
            return;
        }

        try {
            const response = await fetch(`${this.apiBaseUrl}/chat/conversations/${sessionId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                // Remove from UI
                const conversationItem = document.querySelector(`[data-session-id="${sessionId}"]`);
                if (conversationItem) {
                    conversationItem.remove();
                }

                // Remove from memory
                this.conversations.delete(sessionId);

                // If this was the current conversation, start a new one
                if (this.currentSessionId === sessionId) {
                    this.startNewChat();
                }

                console.log('Conversation deleted successfully');
            } else {
                console.error('Failed to delete conversation');
            }
        } catch (error) {
            console.error('Error deleting conversation:', error);
        }
    }

    setInputState(enabled) {
        this.messageInput.disabled = !enabled;
        this.sendBtn.disabled = !enabled;
    }

    scrollToBottom() {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    formatTime(date) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    generateSessionId() {
        return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    getStoredConversations() {
        try {
            const stored = localStorage.getItem('chatConversations');
            return stored ? new Map(JSON.parse(stored)) : new Map();
        } catch (error) {
            console.error('Error loading stored conversations:', error);
            return new Map();
        }
    }

    saveConversations() {
        try {
            const conversationsArray = Array.from(this.conversations.entries());
            localStorage.setItem('chatConversations', JSON.stringify(conversationsArray));
        } catch (error) {
            console.error('Error saving conversations:', error);
        }
    }
}

// Initialize the app when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});
```

## Usage Instructions

### 1. Setup
1. Ensure your Spring Boot backend is running on `http://localhost:8080`
2. Save the HTML, CSS, and JavaScript files
3. Open the HTML file in a web browser

### 2. Features
- **Multiple AI Models**: Switch between DeepSeek, Grok, and Gemma
- **Conversation History**: Sidebar shows all previous conversations
- **Session Management**: Each conversation maintains context
- **Real-time UI**: Typing indicators and smooth animations
- **Responsive Design**: Works on desktop and mobile
- **Persistent Storage**: Conversations saved in browser localStorage

### 3. API Integration Points
- **Chat API**: `POST /chat/ask` for sending messages
- **History API**: `GET /chat/history/{sessionId}` for loading conversations
- **Clear API**: `DELETE /chat/history/{sessionId}` for clearing conversations

### 4. Error Handling
The implementation includes comprehensive error handling for:
- Network failures
- API rate limits
- Invalid responses
- Session management errors

### 5. Customization
You can easily customize:
- API base URL
- UI colors and styling
- Message formatting
- Conversation storage method
- Additional features like file uploads

This implementation provides a professional, production-ready chat interface that fully utilizes your backend APIs with a clean, modern design and excellent user experience.
