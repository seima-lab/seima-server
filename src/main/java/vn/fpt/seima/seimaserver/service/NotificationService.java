package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.response.notification.NotificationResponse;
import vn.fpt.seima.seimaserver.entity.Notification;
import vn.fpt.seima.seimaserver.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {
    


    Page<NotificationResponse> getNotificationsWithFilters(
            Integer userId,
            Boolean isRead,
            NotificationType notificationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
    

    boolean markNotificationAsRead(Integer notificationId, Integer userId);
    

    int markAllNotificationsAsRead(Integer userId);


    long getUnreadNotificationCount(Integer userId);


    boolean deleteNotification(Integer notificationId, Integer userId);
    

    int deleteAllNotifications(Integer userId);


    void sendNotificationToGroupMembers(Integer groupId, Integer senderUserId, String senderUserName, 
                                       NotificationType notificationType, String title, String message, 
                                       String linkToEntity);

    void sendGroupJoinRequestNotification(Integer groupId, Integer requestUserId, String requestUserName);


    void sendPendingApprovalNotificationToUser(Integer groupId, Integer userId, String groupName);


    void sendRejectionNotificationToUser(Integer groupId, Integer userId, String groupName, String rejectedByUserName);


    void sendAcceptanceNotificationToUser(Integer groupId, Integer userId, String groupName, String acceptedByUserName);

} 