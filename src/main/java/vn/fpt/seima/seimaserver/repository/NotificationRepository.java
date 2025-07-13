package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Notification;
import vn.fpt.seima.seimaserver.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId AND n.receiver.userId = :receiverId")
    int markAsReadById(@Param("notificationId") Integer notificationId, @Param("receiverId") Integer receiverId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.userId = :receiverId AND n.isRead = false")
    int markAllAsReadByReceiverId(@Param("receiverId") Integer receiverId);


    @Query("SELECT n FROM Notification n WHERE n.receiver.userId = :receiverId " +
           "AND (:isRead IS NULL OR n.isRead = :isRead) " +
           "AND (:notificationType IS NULL OR n.notificationType = :notificationType) " +
           "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR n.createdAt <= :endDate) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findNotificationsWithFilters(
            @Param("receiverId") Integer receiverId,
            @Param("isRead") Boolean isRead,
            @Param("notificationType") NotificationType notificationType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.receiver.userId = :receiverId AND n.isRead = false")
    long countUnreadByReceiverId(@Param("receiverId") Integer receiverId);
    

    /**
     * Delete specific notification (with security check)
     * @param notificationId notification ID
     * @param receiverId user ID for security
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.notificationId = :notificationId AND n.receiver.userId = :receiverId")
    int deleteByIdAndReceiverId(@Param("notificationId") Integer notificationId, @Param("receiverId") Integer receiverId);
    
    /**
     * Delete all notifications for a user
     * @param receiverId user ID
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.receiver.userId = :receiverId")
    int deleteAllByReceiverId(@Param("receiverId") Integer receiverId);
    


} 