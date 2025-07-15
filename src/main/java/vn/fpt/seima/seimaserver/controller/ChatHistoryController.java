package vn.fpt.seima.seimaserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.chat.CreateChatMessageRequest;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;
import vn.fpt.seima.seimaserver.dto.response.chat.ConversationSummaryResponse;
import vn.fpt.seima.seimaserver.entity.SenderType;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.service.ChatHistoryService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-history")
@Tag(name = "Chat History", description = "Chat history management APIs for chatbot functionality")
public class ChatHistoryController {
    
    private final ChatHistoryService chatHistoryService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new chat message")
    public ApiResponse<ChatMessageResponse> createChatMessage(@Valid @RequestBody CreateChatMessageRequest request) {
        try {
            ChatMessageResponse response = chatHistoryService.createChatMessage(request);
            return new ApiResponse<>(HttpStatus.CREATED.value(), "Chat message created successfully", response);
        } catch (Exception ex) {
            log.error("Error creating chat message", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create chat message: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping
    @Operation(summary = "Get chat history for current user with pagination")
    public ApiResponse<Page<ChatMessageResponse>> getUserChatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> response = chatHistoryService.getUserChatHistory(pageable);
            return new ApiResponse<>(HttpStatus.OK.value(), "Chat history retrieved successfully", response);
        } catch (Exception ex) {
            log.error("Error retrieving chat history", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve chat history: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "Get chat history for a specific conversation")
    public ApiResponse<Page<ChatMessageResponse>> getConversationHistory(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> response = chatHistoryService.getConversationHistory(conversationId, pageable);
            return new ApiResponse<>(HttpStatus.OK.value(), "Conversation history retrieved successfully", response);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            log.error("Error retrieving conversation history", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve conversation history: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/date-range")
    @Operation(summary = "Get chat history within a date range")
    public ApiResponse<Page<ChatMessageResponse>> getChatHistoryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> response = chatHistoryService.getUserChatHistoryByDateRange(startDate, endDate, pageable);
            return new ApiResponse<>(HttpStatus.OK.value(), "Chat history retrieved successfully", response);
        } catch (Exception ex) {
            log.error("Error retrieving chat history by date range", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve chat history: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/sender-type/{senderType}")
    @Operation(summary = "Get chat history filtered by sender type")
    public ApiResponse<Page<ChatMessageResponse>> getChatHistoryBySenderType(
            @PathVariable SenderType senderType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> response = chatHistoryService.getUserChatHistoryBySenderType(senderType, pageable);
            return new ApiResponse<>(HttpStatus.OK.value(), "Chat history retrieved successfully", response);
        } catch (Exception ex) {
            log.error("Error retrieving chat history by sender type", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve chat history: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/conversations")
    @Operation(summary = "Get all conversation summaries for current user")
    public ApiResponse<List<ConversationSummaryResponse>> getUserConversationSummaries() {
        try {
            List<ConversationSummaryResponse> response = chatHistoryService.getUserConversationSummaries();
            return new ApiResponse<>(HttpStatus.OK.value(), "Conversation summaries retrieved successfully", response);
        } catch (Exception ex) {
            log.error("Error retrieving conversation summaries", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve conversation summaries: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/conversation-ids")
    @Operation(summary = "Get all conversation IDs for current user")
    public ApiResponse<List<String>> getUserConversationIds() {
        try {
            List<String> response = chatHistoryService.getUserConversationIds();
            return new ApiResponse<>(HttpStatus.OK.value(), "Conversation IDs retrieved successfully", response);
        } catch (Exception ex) {
            log.error("Error retrieving conversation IDs", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve conversation IDs: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/{chatId}")
    @Operation(summary = "Get a specific chat message by ID")
    public ApiResponse<ChatMessageResponse> getChatMessageById(@PathVariable Integer chatId) {
        try {
            ChatMessageResponse response = chatHistoryService.getChatMessageById(chatId);
            return new ApiResponse<>(HttpStatus.OK.value(), "Chat message retrieved successfully", response);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            log.error("Error retrieving chat message", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve chat message: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/count")
    @Operation(summary = "Get total message count for current user")
    public ApiResponse<Long> getUserTotalMessageCount() {
        try {
            Long count = chatHistoryService.getUserTotalMessageCount();
            return new ApiResponse<>(HttpStatus.OK.value(), "Message count retrieved successfully", count);
        } catch (Exception ex) {
            log.error("Error retrieving message count", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve message count: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/conversation/{conversationId}/count")
    @Operation(summary = "Get message count for a specific conversation")
    public ApiResponse<Long> getConversationMessageCount(@PathVariable String conversationId) {
        try {
            Long count = chatHistoryService.getConversationMessageCount(conversationId);
            return new ApiResponse<>(HttpStatus.OK.value(), "Conversation message count retrieved successfully", count);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            log.error("Error retrieving conversation message count", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve conversation message count: " + ex.getMessage(), null);
        }
    }
    
    @DeleteMapping
    @Operation(summary = "Delete all chat history for current user")
    public ApiResponse<Void> deleteUserChatHistory() {
        try {
            chatHistoryService.deleteUserChatHistory();
            return new ApiResponse<>(HttpStatus.OK.value(), "Chat history deleted successfully", null);
        } catch (Exception ex) {
            log.error("Error deleting chat history", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete chat history: " + ex.getMessage(), null);
        }
    }
    
    @DeleteMapping("/conversation/{conversationId}")
    @Operation(summary = "Delete a specific conversation")
    public ApiResponse<Void> deleteConversation(@PathVariable String conversationId) {
        try {
            chatHistoryService.deleteConversation(conversationId);
            return new ApiResponse<>(HttpStatus.OK.value(), "Conversation deleted successfully", null);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            log.error("Error deleting conversation", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete conversation: " + ex.getMessage(), null);
        }
    }
} 