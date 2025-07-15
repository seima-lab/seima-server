# Chat History API Documentation

This document provides detailed information about the Chat History API endpoints for the chatbot functionality.

## Base URL
```
/api/v1/chat-history
```

## Authentication
All endpoints require authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Endpoints

### 1. Create Chat Message
**POST** `/api/v1/chat-history`

Creates a new chat message in the history.

**Request Body:**
```json
{
  "conversationId": "conv-123-456",
  "senderType": "USER",
  "messageContent": "Hello, how can I help you today?"
}
```

**Response:**
```json
{
  "statusCode": 201,
  "message": "Chat message created successfully",
  "data": {
    "chatId": 1,
    "userId": 123,
    "conversationId": "conv-123-456",
    "senderType": "USER",
    "messageContent": "Hello, how can I help you today?",
    "timestamp": "2024-01-15 10:30:00"
  }
}
```

### 2. Get User Chat History
**GET** `/api/v1/chat-history`

Retrieves all chat messages for the current user with pagination.

**Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size

**Response:**
```json
{
  "statusCode": 200,
  "message": "Chat history retrieved successfully",
  "data": {
    "content": [
      {
        "chatId": 1,
        "userId": 123,
        "conversationId": "conv-123-456",
        "senderType": "USER",
        "messageContent": "Hello, how can I help you today?",
        "timestamp": "2024-01-15 10:30:00"
      }
    ],
    "pageable": {
      "sort": {...},
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 3. Get Conversation History
**GET** `/api/v1/chat-history/conversation/{conversationId}`

Retrieves chat messages for a specific conversation.

**Parameters:**
- `conversationId` (path) - The conversation ID
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size

**Response:** Same structure as Get User Chat History

### 4. Get Chat History by Date Range
**GET** `/api/v1/chat-history/date-range`

Retrieves chat messages within a specified date range.

**Parameters:**
- `startDate` (required) - Start date in ISO format (e.g., 2024-01-15T00:00:00)
- `endDate` (required) - End date in ISO format (e.g., 2024-01-16T23:59:59)
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size

**Response:** Same structure as Get User Chat History

### 5. Get Chat History by Sender Type
**GET** `/api/v1/chat-history/sender-type/{senderType}`

Retrieves chat messages filtered by sender type.

**Parameters:**
- `senderType` (path) - Either "USER" or "AI"
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size

**Response:** Same structure as Get User Chat History

### 6. Get Conversation Summaries
**GET** `/api/v1/chat-history/conversations`

Retrieves summaries of all conversations for the current user.

**Response:**
```json
{
  "statusCode": 200,
  "message": "Conversation summaries retrieved successfully",
  "data": [
    {
      "conversationId": "conv-123-456",
      "lastMessageContent": "Thank you for your help!",
      "lastMessageSenderType": "USER",
      "messageCount": 15,
      "lastMessageTimestamp": "2024-01-15 10:30:00"
    }
  ]
}
```

### 7. Get Conversation IDs
**GET** `/api/v1/chat-history/conversation-ids`

Retrieves all conversation IDs for the current user.

**Response:**
```json
{
  "statusCode": 200,
  "message": "Conversation IDs retrieved successfully",
  "data": [
    "conv-123-456",
    "conv-789-012"
  ]
}
```

### 8. Get Chat Message by ID
**GET** `/api/v1/chat-history/{chatId}`

Retrieves a specific chat message by its ID.

**Parameters:**
- `chatId` (path) - The chat message ID

**Response:**
```json
{
  "statusCode": 200,
  "message": "Chat message retrieved successfully",
  "data": {
    "chatId": 1,
    "userId": 123,
    "conversationId": "conv-123-456",
    "senderType": "USER",
    "messageContent": "Hello, how can I help you today?",
    "timestamp": "2024-01-15 10:30:00"
  }
}
```

### 9. Get User Total Message Count
**GET** `/api/v1/chat-history/count`

Retrieves the total number of messages for the current user.

**Response:**
```json
{
  "statusCode": 200,
  "message": "Message count retrieved successfully",
  "data": 150
}
```

### 10. Get Conversation Message Count
**GET** `/api/v1/chat-history/conversation/{conversationId}/count`

Retrieves the total number of messages in a specific conversation.

**Parameters:**
- `conversationId` (path) - The conversation ID

**Response:**
```json
{
  "statusCode": 200,
  "message": "Conversation message count retrieved successfully",
  "data": 25
}
```

### 11. Delete User Chat History
**DELETE** `/api/v1/chat-history`

Deletes all chat history for the current user.

**Response:**
```json
{
  "statusCode": 200,
  "message": "Chat history deleted successfully",
  "data": null
}
```

### 12. Delete Conversation
**DELETE** `/api/v1/chat-history/conversation/{conversationId}`

Deletes a specific conversation.

**Parameters:**
- `conversationId` (path) - The conversation ID to delete

**Response:**
```json
{
  "statusCode": 200,
  "message": "Conversation deleted successfully",
  "data": null
}
```

## Error Responses

### 400 Bad Request
```json
{
  "statusCode": 400,
  "message": "Validation error message",
  "data": null
}
```

### 404 Not Found
```json
{
  "statusCode": 404,
  "message": "Resource not found",
  "data": null
}
```

### 500 Internal Server Error
```json
{
  "statusCode": 500,
  "message": "Internal server error message",
  "data": null
}
```

## Data Models

### SenderType Enum
- `USER` - Message sent by the user
- `AI` - Message sent by the AI/chatbot

### CreateChatMessageRequest
- `conversationId` (String, optional, max 255 characters) - The conversation ID
- `senderType` (SenderType, required) - Who sent the message
- `messageContent` (String, required, max 10000 characters) - The message content

### ChatMessageResponse
- `chatId` (Integer) - Unique message ID
- `userId` (Integer) - User ID who owns the message
- `conversationId` (String) - Conversation ID
- `senderType` (SenderType) - Who sent the message
- `messageContent` (String) - The message content
- `timestamp` (LocalDateTime) - When the message was created

### ConversationSummaryResponse
- `conversationId` (String) - The conversation ID
- `lastMessageContent` (String) - Content of the last message
- `lastMessageSenderType` (SenderType) - Who sent the last message
- `messageCount` (Long) - Total number of messages in the conversation
- `lastMessageTimestamp` (LocalDateTime) - Timestamp of the last message

## Usage Examples

### Creating a User Message
```bash
curl -X POST "http://localhost:8080/api/v1/chat-history" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-123-456",
    "senderType": "USER",
    "messageContent": "What is the weather like today?"
  }'
```

### Creating an AI Response
```bash
curl -X POST "http://localhost:8080/api/v1/chat-history" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-123-456",
    "senderType": "AI",
    "messageContent": "I can help you with weather information. Please provide your location."
  }'
```

### Getting Conversation History
```bash
curl -X GET "http://localhost:8080/api/v1/chat-history/conversation/conv-123-456?page=0&size=20" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Getting User's All Conversations
```bash
curl -X GET "http://localhost:8080/api/v1/chat-history/conversations" \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Notes

1. All endpoints require authentication using JWT tokens
2. The API uses pagination for list endpoints to handle large datasets efficiently
3. Users can only access their own chat history
4. The `conversationId` can be used to group related messages together
5. Messages are automatically timestamped when created
6. The API supports both user messages and AI responses through the `senderType` field 