# Chat History API Documentation

## Overview

The Chat History API provides **read-only access** to user's continuous chat history. Each user has a single, unified chat history from the beginning to the end. This API is designed for retrieving and managing existing chat conversations.

## Key Features

### Continuous Conversation Model
- Each user has **one continuous chat history**
- All messages are part of a single conversation thread
- Simplified API focused on conversation retrieval and management
- No message creation via API (messages are created through other services)

## API Endpoints

### 1. Get Complete Chat History

**GET** `/api/v1/chat-history?page=0&size=50`

Retrieves the user's complete chat history with pagination.

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 50)

**Response:**
```json
{
  "status": 200,
  "message": "Chat history retrieved successfully",
  "data": {
    "content": [
      {
        "chatId": 123,
        "userId": 456,
        "senderType": "USER",
        "messageContent": "Hello, how can you help me?",
        "timestamp": "2024-01-15T10:30:00"
      },
      {
        "chatId": 124,
        "userId": 456,
        "senderType": "AI",
        "messageContent": "Hello! I'm here to help you with your financial questions.",
        "timestamp": "2024-01-15T10:30:05"
      }
    ],
    "totalElements": 25,
    "totalPages": 1,
    "size": 50,
    "number": 0
  }
}
```

### 2. Get Recent Messages

**GET** `/api/v1/chat-history/recent?limit=10`

Retrieves the most recent messages from the user's chat history.

**Query Parameters:**
- `limit` (optional): Number of recent messages to retrieve (default: 10)

**Response:**
```json
{
  "status": 200,
  "message": "Recent messages retrieved successfully",
  "data": [
    {
      "chatId": 123,
      "userId": 456,
      "senderType": "USER",
      "messageContent": "What's my current balance?",
      "timestamp": "2024-01-15T10:35:00"
    },
    {
      "chatId": 124,
      "userId": 456,
      "senderType": "AI",
      "messageContent": "Your current balance is $1,250.00",
      "timestamp": "2024-01-15T10:35:05"
    }
  ]
}
```

### 3. Get Message Count

**GET** `/api/v1/chat-history/count`

Returns the total number of messages in the user's chat history.

**Response:**
```json
{
  "status": 200,
  "message": "Message count retrieved successfully",
  "data": 25
}
```

### 4. Clear Chat History

**DELETE** `/api/v1/chat-history`

Clears the entire chat history for the current user.

**Response:**
```json
{
  "status": 200,
  "message": "Chat history cleared successfully",
  "data": null
}
```

## Usage Examples

### Retrieving Conversation Context

1. **Get recent messages for context:**
   ```bash
   GET /api/v1/chat-history/recent?limit=5
   ```

2. **Get full conversation history:**
   ```bash
   GET /api/v1/chat-history?page=0&size=100
   ```

3. **Check conversation length:**
   ```bash
   GET /api/v1/chat-history/count
   ```

4. **Clear conversation history:**
   ```bash
   DELETE /api/v1/chat-history
   ```

## Benefits of Read-Only Chat History API

1. **Security**: No risk of unauthorized message creation
2. **Separation of Concerns**: Message creation handled by dedicated services
3. **Simplified Access**: Focus on retrieval and management only
4. **Better Control**: Centralized message creation logic
5. **Audit Trail**: Clear separation between creation and retrieval

## Data Model

The `ChatHistory` entity represents a continuous conversation:

```java
@Entity
@Table(name = "chat_history")
public class ChatHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer chatId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType; // USER or AI
    
    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;
    
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
```

## Message Creation

**Note**: This API does not provide endpoints for creating chat messages. Messages are created through:

1. **AI Service**: When AI generates responses
2. **Chat Service**: When processing user inputs
3. **Other Internal Services**: For system-generated messages

## Error Handling

All endpoints return consistent error responses:

```json
{
  "status": 500,
  "message": "Failed to retrieve chat history: User not found",
  "data": null
}
```

Common error scenarios:
- User not authenticated
- User not found
- Database connection issues
- Invalid pagination parameters

## Security Considerations

- All endpoints require user authentication
- Users can only access their own chat history
- No message creation capabilities
- Read-only access to conversation data
- No cross-user data access possible

## Integration Notes

When integrating with this API:

1. **Frontend Applications**: Use for displaying conversation history
2. **AI Services**: Use recent messages for context retrieval
3. **Analytics**: Use for conversation analysis and insights
4. **User Management**: Use for user conversation management 