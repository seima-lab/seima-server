package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.chat.CreateChatMessageRequest;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;
import vn.fpt.seima.seimaserver.entity.SenderType;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatHistoryService {
    
    /**
     * Create a new chat message
     * @param request the chat message request
     * @return created chat message response
     */
    ChatMessageResponse createChatMessage(CreateChatMessageRequest request);
    
    /**
     * Get all chat messages for the current user with pagination
     * @param pageable pagination parameters
     * @return paginated chat messages
     */
    Page<ChatMessageResponse> getUserChatHistory(Pageable pageable);
    
    /**
     * Get chat messages for the current user within a date range
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination parameters
     * @return paginated chat messages within date range
     */
    Page<ChatMessageResponse> getUserChatHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Get chat messages for the current user by sender type
     * @param senderType the sender type (USER or AI)
     * @param pageable pagination parameters
     * @return paginated chat messages by sender type
     */
    Page<ChatMessageResponse> getUserChatHistoryBySenderType(SenderType senderType, Pageable pageable);
    
    /**
     * Delete all chat history for the current user
     */
    void deleteUserChatHistory();
    
    /**
     * Get chat message by ID (with user ownership check)
     * @param chatId the chat ID
     * @return chat message response
     */
    ChatMessageResponse getChatMessageById(Integer chatId);
    
    /**
     * Get total message count for the current user
     * @return total message count
     */
    Long getUserTotalMessageCount();
} 