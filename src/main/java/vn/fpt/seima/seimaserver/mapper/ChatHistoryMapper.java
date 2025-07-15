package vn.fpt.seima.seimaserver.mapper;

import org.springframework.stereotype.Component;
import vn.fpt.seima.seimaserver.dto.request.chat.CreateChatMessageRequest;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;
import vn.fpt.seima.seimaserver.dto.response.chat.ConversationSummaryResponse;
import vn.fpt.seima.seimaserver.entity.ChatHistory;
import vn.fpt.seima.seimaserver.entity.User;

@Component
public class ChatHistoryMapper {
    
    public ChatHistory toEntity(CreateChatMessageRequest request, User user) {
        return ChatHistory.builder()
                .user(user)
                .conversationId(request.getConversationId())
                .senderType(request.getSenderType())
                .messageContent(request.getMessageContent())
                .build();
    }
    
    public ChatMessageResponse toResponse(ChatHistory chatHistory) {
        return ChatMessageResponse.builder()
                .chatId(chatHistory.getChatId())
                .userId(chatHistory.getUser().getUserId())
                .conversationId(chatHistory.getConversationId())
                .senderType(chatHistory.getSenderType())
                .messageContent(chatHistory.getMessageContent())
                .timestamp(chatHistory.getTimestamp())
                .build();
    }
    
    public ConversationSummaryResponse toConversationSummary(ChatHistory lastMessage, Long messageCount) {
        return ConversationSummaryResponse.builder()
                .conversationId(lastMessage.getConversationId())
                .lastMessageContent(lastMessage.getMessageContent())
                .lastMessageSenderType(lastMessage.getSenderType())
                .messageCount(messageCount)
                .lastMessageTimestamp(lastMessage.getTimestamp())
                .build();
    }
} 