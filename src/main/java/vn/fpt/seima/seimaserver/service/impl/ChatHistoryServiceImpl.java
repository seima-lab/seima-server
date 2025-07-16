package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.chat.CreateChatMessageRequest;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;
import vn.fpt.seima.seimaserver.entity.ChatHistory;
import vn.fpt.seima.seimaserver.entity.SenderType;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.ChatHistoryMapper;
import vn.fpt.seima.seimaserver.repository.ChatHistoryRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.ChatHistoryService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatHistoryServiceImpl implements ChatHistoryService {
    
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final ChatHistoryMapper chatHistoryMapper;
    
    @Override
    public ChatMessageResponse createChatMessage(CreateChatMessageRequest request) {
        log.info("Creating chat message for user: {}", UserUtils.getCurrentUserId());
        
        User currentUser = getCurrentUser();
        ChatHistory chatHistory = chatHistoryMapper.toEntity(request, currentUser);
        ChatHistory savedChatHistory = chatHistoryRepository.save(chatHistory);
        
        log.info("Chat message created successfully with ID: {}", savedChatHistory.getChatId());
        return chatHistoryMapper.toResponse(savedChatHistory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getUserChatHistory(Pageable pageable) {
        log.info("Getting chat history for user: {}", UserUtils.getCurrentUserId());
        
        Integer currentUserId = UserUtils.getCurrentUserId();
        Page<ChatHistory> chatHistoryPage = chatHistoryRepository.findByUserIdOrderByTimestampDesc(currentUserId, pageable);
        
        return chatHistoryPage.map(chatHistoryMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getUserChatHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.info("Getting chat history for user: {} between {} and {}", UserUtils.getCurrentUserId(), startDate, endDate);
        
        Integer currentUserId = UserUtils.getCurrentUserId();
        Page<ChatHistory> chatHistoryPage = chatHistoryRepository.findByUserIdAndTimestampBetween(currentUserId, startDate, endDate, pageable);
        
        return chatHistoryPage.map(chatHistoryMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getUserChatHistoryBySenderType(SenderType senderType, Pageable pageable) {
        log.info("Getting chat history for user: {} with sender type: {}", UserUtils.getCurrentUserId(), senderType);
        
        Integer currentUserId = UserUtils.getCurrentUserId();
        Page<ChatHistory> chatHistoryPage = chatHistoryRepository.findByUserIdAndSenderType(currentUserId, senderType, pageable);
        
        return chatHistoryPage.map(chatHistoryMapper::toResponse);
    }
    
    @Override
    public void deleteUserChatHistory() {
        log.info("Deleting all chat history for user: {}", UserUtils.getCurrentUserId());
        
        Integer currentUserId = UserUtils.getCurrentUserId();
        chatHistoryRepository.deleteByUserId(currentUserId);
        
        log.info("All chat history deleted for user: {}", currentUserId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ChatMessageResponse getChatMessageById(Integer chatId) {
        log.info("Getting chat message with ID: {} for user: {}", chatId, UserUtils.getCurrentUserId());
        
        ChatHistory chatHistory = chatHistoryRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat message not found with ID: " + chatId));
        
        // Verify that the message belongs to the current user
        Integer currentUserId = UserUtils.getCurrentUserId();
        if (!chatHistory.getUser().getUserId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Chat message not found or doesn't belong to current user");
        }
        
        return chatHistoryMapper.toResponse(chatHistory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getUserTotalMessageCount() {
        log.info("Getting total message count for user: {}", UserUtils.getCurrentUserId());
        
        Integer currentUserId = UserUtils.getCurrentUserId();
        return chatHistoryRepository.countByUserId(currentUserId);
    }
    
    private User getCurrentUser() {
        Integer currentUserId = UserUtils.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));
    }
} 