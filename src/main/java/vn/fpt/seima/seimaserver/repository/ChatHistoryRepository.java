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

import java.time.LocalDateTime;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {
    
    /**
     * Find all non-deleted chat messages for a specific user with pagination
     * Ordered by timestamp descending (newest first)
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return Page of ChatHistory ordered by timestamp descending
     */
    @Query("SELECT c FROM ChatHistory c WHERE c.user.userId = :userId AND c.deleted = false ORDER BY c.timestamp DESC")
    Page<ChatHistory> findByUserIdOrderByTimestampDesc(@Param("userId") Integer userId, Pageable pageable);
    
    /**
     * Soft delete all chat history for a specific user
     * @param userId the user ID
     * @param deletedAt the deletion timestamp
     */
    @Modifying
    @Transactional
    @Query("UPDATE ChatHistory c SET c.deleted = true, c.deletedAt = :deletedAt WHERE c.user.userId = :userId AND c.deleted = false")
    void softDeleteByUserId(@Param("userId") Integer userId, @Param("deletedAt") LocalDateTime deletedAt);
    
    /**
     * Count total non-deleted messages for a user
     * @param userId the user ID
     * @return count of non-deleted messages
     */
    @Query("SELECT COUNT(c) FROM ChatHistory c WHERE c.user.userId = :userId AND c.deleted = false")
    Long countByUserId(@Param("userId") Integer userId);
    
    /**
     * Hard delete all chat history for a specific user (for cleanup purposes)
     * @param userId the user ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatHistory c WHERE c.user.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
} 