package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.response.chat.ChatMessageResponse;
import vn.fpt.seima.seimaserver.entity.ChatHistory;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.mapper.ChatHistoryMapper;
import vn.fpt.seima.seimaserver.repository.ChatHistoryRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.ChatHistoryService;
import vn.fpt.seima.seimaserver.util.UserUtils;

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
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getUserChatHistory(Pageable pageable) {
        User currentUser = getCurrentUser();
        log.info("Getting complete chat history for user: {}", currentUser.getUserId());
        
        Page<ChatHistory> chatHistoryPage = chatHistoryRepository.findByUserIdOrderByTimestampDesc(currentUser.getUserId(), pageable);
        
        return chatHistoryPage.map(chatHistoryMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentMessages(int limit) {
        User currentUser = getCurrentUser();
        log.info("Getting {} recent messages for user: {}", limit, currentUser.getUserId());
        
        Pageable pageable = PageRequest.of(0, limit);
        Page<ChatHistory> chatHistoryPage = chatHistoryRepository.findByUserIdOrderByTimestampDesc(currentUser.getUserId(), pageable);
        
        return chatHistoryPage.getContent().stream()
                .map(chatHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void clearUserChatHistory() {
        User currentUser = getCurrentUser();
        log.info("Clearing entire chat history for user: {}", currentUser.getUserId());
        
        chatHistoryRepository.deleteByUserId(currentUser.getUserId());
        
        log.info("Chat history cleared for user: {}", currentUser.getUserId());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getUserTotalMessageCount() {
        User currentUser = getCurrentUser();
        log.info("Getting total message count for user: {}", currentUser.getUserId());
        
        return chatHistoryRepository.countByUserId(currentUser.getUserId());
    }
    
    private User getCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated or not found");
        }
        return currentUser;
    }
} 