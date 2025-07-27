package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;

import java.util.List;

public interface ChatHistoryService {
    
    /**
     * Get all messages from user's continuous chat history with pagination
     * @param pageable pagination parameters
     * @return paginated chat messages
     */
    Page<ChatMessageResponse> getUserChatHistory(Pageable pageable);
    
    /**
     * Get recent messages from user's chat history
     * @param limit number of recent messages to retrieve
     * @return list of recent chat messages
     */
    List<ChatMessageResponse> getRecentMessages(int limit);
    
    /**
     * Clear entire chat history for the current user
     */
    void clearUserChatHistory();
    
    /**
     * Get total message count in user's chat history
     * @return total message count
     */
    Long getUserTotalMessageCount();
} 