package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.ChatHistory;
import vn.fpt.seima.seimaserver.entity.SenderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {
    
    /**
     * Find all chat messages for a specific user with pagination
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return Page of ChatHistory ordered by timestamp descending
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.user.userId = :userId ORDER BY c.timestamp DESC")
    Page<ChatHistory> findByUserIdOrderByTimestampDesc(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Find all chat messages for a specific conversation with pagination
     * @param conversationId the conversation ID
     * @param pageable pagination parameters
     * @return Page of ChatHistory ordered by timestamp ascending
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.conversationId = :conversationId ORDER BY c.timestamp ASC")
    Page<ChatHistory> findByConversationIdOrderByTimestampAsc(@Param("conversationId") String conversationId, Pageable pageable);
    
    /**
     * Find all chat messages for a specific user and conversation
     * @param userId the user ID
     * @param conversationId the conversation ID
     * @param pageable pagination parameters
     * @return Page of ChatHistory ordered by timestamp ascending
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.user.userId = :userId AND c.conversationId = :conversationId ORDER BY c.timestamp ASC")
    Page<ChatHistory> findByUserIdAndConversationIdOrderByTimestampAsc(@Param("userId") Integer userId, @Param("conversationId") String conversationId, Pageable pageable);
    
    /**
     * Find all chat messages for a specific user within a date range
     * @param userId the user ID
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination parameters
     * @return Page of ChatHistory ordered by timestamp descending
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.user.userId = :userId AND c.timestamp BETWEEN :startDate AND :endDate ORDER BY c.timestamp DESC")
    Page<ChatHistory> findByUserIdAndTimestampBetween(@Param("userId") Integer userId, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate, 
                                                     Pageable pageable);
    
    /**
     * Find all chat messages for a specific user and sender type
     * @param userId the user ID
     * @param senderType the sender type (USER or AI)
     * @param pageable pagination parameters
     * @return Page of ChatHistory ordered by timestamp descending
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.user.userId = :userId AND c.senderType = :senderType ORDER BY c.timestamp DESC")
    Page<ChatHistory> findByUserIdAndSenderType(@Param("userId") Integer userId, 
                                              @Param("senderType") SenderType senderType, 
                                              Pageable pageable);
    
    /**
     * Get all unique conversation IDs for a user
     * @param userId the user ID
     * @return List of conversation IDs
     */
    @Query("SELECT DISTINCT c.conversationId FROM ChatHistory c WHERE c.user.userId = :userId AND c.conversationId IS NOT NULL ORDER BY MAX(c.timestamp) DESC")
    List<String> findDistinctConversationIdsByUserId(@Param("userId") Integer userId);
    
    /**
     * Get the most recent message for each conversation for a user
     * @param userId the user ID
     * @return List of ChatHistory representing the latest message in each conversation
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.user.userId = :userId AND c.timestamp = (" +
           "SELECT MAX(c2.timestamp) FROM ChatHistory c2 WHERE c2.conversationId = c.conversationId AND c2.user.userId = :userId) " +
           "ORDER BY c.timestamp DESC")
    List<ChatHistory> findLatestMessagesByUserIdGroupByConversation(@Param("userId") Integer userId);
    
    /**
     * Delete all chat history for a specific user
     * @param userId the user ID
     */
    void deleteByUserId(@Param("userId") Integer userId);
    
    /**
     * Delete all chat history for a specific conversation
     * @param conversationId the conversation ID
     */
    void deleteByConversationId(@Param("conversationId") String conversationId);
    
    /**
     * Count total messages for a user
     * @param userId the user ID
     * @return count of messages
     */
    @Query("SELECT COUNT(c) FROM ChatHistory c WHERE c.user.userId = :userId")
    Long countByUserId(@Param("userId") Integer userId);
    
    /**
     * Count total messages for a conversation
     * @param conversationId the conversation ID
     * @return count of messages
     */
    @Query("SELECT COUNT(c) FROM ChatHistory c WHERE c.conversationId = :conversationId")
    Long countByConversationId(@Param("conversationId") String conversationId);
} 