package vn.fpt.seima.seimaserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;
import vn.fpt.seima.seimaserver.service.ChatHistoryService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-history")
@Tag(name = "chat-history-controller", description = "Chat history management APIs for continuous conversation")
public class ChatHistoryController {
    
    private final ChatHistoryService chatHistoryService;
    
    @GetMapping
    @Operation(summary = "Get user's complete chat history with pagination")
    public ApiResponse<Page<ChatMessageResponse>> getUserChatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessageResponse> response = chatHistoryService.getUserChatHistory(pageable);
            return new ApiResponse<>(HttpStatus.OK.value(), "Chat history retrieved successfully", response);
        } catch (Exception ex) {
            log.error("Error retrieving chat history", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve chat history: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/recent")
    @Operation(summary = "Get recent messages from user's chat history")
    public ApiResponse<List<ChatMessageResponse>> getRecentMessages(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ChatMessageResponse> response = chatHistoryService.getRecentMessages(limit);
            return new ApiResponse<>(HttpStatus.OK.value(), "Recent messages retrieved successfully", response);
        } catch (Exception ex) {
            log.error("Error retrieving recent messages", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve recent messages: " + ex.getMessage(), null);
        }
    }
    
    @GetMapping("/count")
    @Operation(summary = "Get total message count in user's chat history")
    public ApiResponse<Long> getUserTotalMessageCount() {
        try {
            Long count = chatHistoryService.getUserTotalMessageCount();
            return new ApiResponse<>(HttpStatus.OK.value(), "Message count retrieved successfully", count);
        } catch (Exception ex) {
            log.error("Error retrieving message count", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve message count: " + ex.getMessage(), null);
        }
    }
    
    @DeleteMapping
    @Operation(summary = "Clear entire chat history for current user")
    public ApiResponse<Void> clearUserChatHistory() {
        try {
            chatHistoryService.clearUserChatHistory();
            return new ApiResponse<>(HttpStatus.OK.value(), "Chat history cleared successfully", null);
        } catch (Exception ex) {
            log.error("Error clearing chat history", ex);
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to clear chat history: " + ex.getMessage(), null);
        }
    }
} 