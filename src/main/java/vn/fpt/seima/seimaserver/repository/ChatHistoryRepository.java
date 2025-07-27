package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.entity.ChatHistory;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {
    
    /**
     * Find all chat messages for a specific user with pagination
     * Ordered by timestamp descending (newest first)
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return Page of ChatHistory ordered by timestamp descending
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.user.userId = :userId ORDER BY c.timestamp DESC")
    Page<ChatHistory> findByUserIdOrderByTimestampDesc(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Delete all chat history for a specific user
     * @param userId the user ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatHistory c WHERE c.user.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
    
    /**
     * Count total messages for a user
     * @param userId the user ID
     * @return count of messages
     */
    @Query("SELECT COUNT(c) FROM ChatHistory c WHERE c.user.userId = :userId")
    Long countByUserId(@Param("userId") Integer userId);
} 