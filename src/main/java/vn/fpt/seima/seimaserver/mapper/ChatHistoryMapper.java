package vn.fpt.seima.seimaserver.mapper;

import org.springframework.stereotype.Component;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;
import vn.fpt.seima.seimaserver.entity.ChatHistory;

@Component
public class ChatHistoryMapper {

    
    public ChatMessageResponse toResponse(ChatHistory chatHistory) {
        // Only map non-deleted records
        if (chatHistory.getDeleted() != null && chatHistory.getDeleted()) {
            return null; // Return null for deleted records
        }
        
        return ChatMessageResponse.builder()
                .chatId(chatHistory.getChatId())
                .userId(chatHistory.getUser().getUserId())
                .senderType(chatHistory.getSenderType())
                .messageContent(chatHistory.getMessageContent())
                .timestamp(chatHistory.getTimestamp())
                .build();
    }
} 