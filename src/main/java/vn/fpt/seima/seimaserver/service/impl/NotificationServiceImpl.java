package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.response.notification.NotificationResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.entity.Notification;
import vn.fpt.seima.seimaserver.entity.NotificationType;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.repository.NotificationRepository;
import vn.fpt.seima.seimaserver.repository.UserDeviceRepository;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.FcmService;
import vn.fpt.seima.seimaserver.service.NotificationCacheService;
import vn.fpt.seima.seimaserver.service.NotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    // Constants for notification
    private static final String JOIN_REQUEST_TITLE = "Group Join Request";
    private static final String JOIN_REQUEST_BODY_TEMPLATE = "%s want to join group";
    private static final int BATCH_SIZE = 100; // Batch size for bulk operations
    
    private final NotificationRepository notificationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final FcmService fcmService;
    private final NotificationCacheService notificationCacheService;
    

    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsWithFilters(
            Integer userId,
            Boolean isRead,
            NotificationType notificationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        logger.info("Fetching notifications with filters for user: {} - isRead: {}, type: {}, startDate: {}, endDate: {}", 
                   userId, isRead, notificationType, startDate, endDate);
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User with id {} not found", userId);
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Validate date range
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            logger.error("Invalid date range: startDate {} is after endDate {}", startDate, endDate);
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        // Fetch filtered notifications
        Page<Notification> notificationPage = notificationRepository.findNotificationsWithFilters(
                userId, isRead, notificationType, startDate, endDate, pageable);
        
        // Convert to response DTOs
        Page<NotificationResponse> responsePage = notificationPage.map(this::convertToNotificationResponse);
        
        logger.info("Retrieved {} notifications (page {}/{}) for user: {}", 
                   responsePage.getNumberOfElements(), responsePage.getNumber() + 1, 
                   responsePage.getTotalPages(), userId);
        
        return responsePage;
    }
    

    
    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(Integer userId) {
        logger.info("Getting unread notification count for user: {}", userId);
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User with id {} not found", userId);
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Try cache first, fallback to database
        Long cachedCount = null;
        try {
            cachedCount = notificationCacheService.getUnreadCountFromCache(userId);
            if (cachedCount != null) {
                logger.info("Cache hit for user {}: {}", userId, cachedCount);
                return cachedCount;
            }
        } catch (Exception e) {
            logger.warn("Cache error for user {}: {}", userId, e.getMessage());
        }
        
        // Cache miss or error - get from database and cache
        long count = notificationRepository.countUnreadByReceiverId(userId);
        try {
            notificationCacheService.setUnreadCountInCache(userId, count);
        } catch (Exception e) {
            logger.warn("Failed to set cache for user {}: {}", userId, e.getMessage());
        }
        logger.info("Cache miss for user {}: {}", userId, count);
        return count;
    }
    

    

    
    @Override
    @Transactional
    public boolean deleteNotification(Integer notificationId, Integer userId) {
        logger.info("Deleting notification {} for user: {}", notificationId, userId);
        
        // Validate input
        if (notificationId == null || userId == null) {
            logger.error("Invalid input: notificationId={}, userId={}", notificationId, userId);
            throw new IllegalArgumentException("Notification ID and User ID cannot be null");
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User with id {} not found", userId);
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Delete notification (only if it belongs to the user)
        int deletedCount = notificationRepository.deleteByIdAndReceiverId(notificationId, userId);
        
        if (deletedCount > 0) {
            // Decrement cache count
            try {
                notificationCacheService.decrementUnreadCount(userId);
            } catch (Exception e) {
                logger.warn("Failed to decrement cache for user {}: {}", userId, e.getMessage());
            }
            logger.info("Successfully deleted notification {} for user: {}", notificationId, userId);
            return true;
        } else {
            logger.warn("No notification found with id {} for user: {}", notificationId, userId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public int deleteAllNotifications(Integer userId) {
        logger.info("Deleting all notifications for user: {}", userId);
        
        // Validate input
        if (userId == null) {
            logger.error("User ID cannot be null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User with id {} not found", userId);
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Delete all notifications
        int deletedCount = notificationRepository.deleteAllByReceiverId(userId);
        
        // Reset cache count
        try {
            notificationCacheService.resetUnreadCount(userId);
        } catch (Exception e) {
            logger.warn("Failed to reset cache for user {}: {}", userId, e.getMessage());
        }
        logger.info("Successfully deleted {} notifications for user: {}", deletedCount, userId);
        return deletedCount;
    }

    
    @Override
    @Transactional
    public boolean markNotificationAsRead(Integer notificationId, Integer userId) {
        logger.info("Marking notification {} as read for user: {}", notificationId, userId);
        
        // Validate input
        if (notificationId == null || userId == null) {
            logger.error("Invalid input: notificationId={}, userId={}", notificationId, userId);
            throw new IllegalArgumentException("Notification ID and User ID cannot be null");
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User with id {} not found", userId);
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Update notification as read (only if it belongs to the user)
        int updatedCount = notificationRepository.markAsReadById(notificationId, userId);
        
        if (updatedCount > 0) {
            // Decrement cache count
            try {
                notificationCacheService.decrementUnreadCount(userId);
            } catch (Exception e) {
                logger.warn("Failed to decrement cache for user {}: {}", userId, e.getMessage());
            }
            logger.info("Successfully marked notification {} as read for user: {}", notificationId, userId);
            return true;
        } else {
            logger.warn("No notification found with id {} for user: {} or already read", notificationId, userId);
            return false;
        }
    }
    
    @Override
    @Transactional
    public int markAllNotificationsAsRead(Integer userId) {
        logger.info("Marking all notifications as read for user: {}", userId);
        
        // Validate input
        if (userId == null) {
            logger.error("User ID cannot be null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            logger.error("User with id {} not found", userId);
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Update all unread notifications as read
        int updatedCount = notificationRepository.markAllAsReadByReceiverId(userId);
        
        // Reset cache count
        try {
            notificationCacheService.resetUnreadCount(userId);
        } catch (Exception e) {
            logger.warn("Failed to reset cache for user {}: {}", userId, e.getMessage());
        }
        logger.info("Successfully marked {} notifications as read for user: {}", updatedCount, userId);
        return updatedCount;
    }
    
    /**
     * Convert Notification entity to NotificationResponse DTO
     * @param notification the notification entity
     * @return NotificationResponse DTO
     */
    private NotificationResponse convertToNotificationResponse(Notification notification) {
        NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .linkToEntity(notification.getLinkToEntity())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt());
        
        // Add sender information if exists (null for system notifications)
        if (notification.getSender() != null) {
            NotificationResponse.SenderInfo senderInfo = NotificationResponse.SenderInfo.builder()
                    .userId(notification.getSender().getUserId())
                    .userFullName(notification.getSender().getUserFullName())
                    .userAvatarUrl(notification.getSender().getUserAvatarUrl())
                    .build();
            builder.sender(senderInfo);
        }
        
        return builder.build();
    }

    @Override
    @Transactional
    public void sendNotificationToGroupMembers(Integer groupId, Integer senderUserId, String senderUserName, 
                                             NotificationType notificationType, String title, String message, 
                                             String linkToEntity) {
        logger.info("Sending notification to group members for group: {}, user: {}, type: {}", 
                   groupId, senderUserId, notificationType);
        
        try {
            // Validate input
            validateGenericNotificationInput(groupId, senderUserId, senderUserName, notificationType, title, message);
            
            // Get admin and owner members
            List<GroupMember> adminAndOwnerMembers = groupMemberRepository.findAdminAndOwnerMembers(
                groupId, GroupMemberStatus.ACTIVE);
            
            if (adminAndOwnerMembers.isEmpty()) {
                logger.warn("No admin or owner found for group: {}", groupId);
                return;
            }
            
            // Get sender user
            Optional<User> senderUserOpt = userRepository.findById(senderUserId);
            if (senderUserOpt.isEmpty()) {
                logger.warn("Sender user not found: {}", senderUserId);
                return;
            }
            User senderUser = senderUserOpt.get();
            
            // Process notifications asynchronously for better performance
            processNotificationsAsync(adminAndOwnerMembers, senderUser, groupId, notificationType, 
                                    title, message, linkToEntity, senderUserName);
            
            logger.info("Successfully initiated notification to {} admin/owner users", 
                adminAndOwnerMembers.size());
            
        } catch (Exception e) {
            logger.error("Error sending notification for group: {}, user: {}, type: {}", 
                groupId, senderUserId, notificationType, e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public void sendGroupJoinRequestNotification(Integer groupId, Integer requestUserId, String requestUserName) {
        logger.info("Sending group join request notification for group: {}, user: {}", groupId, requestUserId);
        
        String linkToEntity = "seimaapp://groups/" + groupId + "/pending-members";
        String message = String.format(JOIN_REQUEST_BODY_TEMPLATE, requestUserName);
        
        sendNotificationToGroupMembers(groupId, requestUserId, requestUserName, 
                                     NotificationType.GROUP_JOIN_REQUEST, JOIN_REQUEST_TITLE, 
                                     message, linkToEntity);
    }

    @Override
    @Transactional
    public void  sendPendingApprovalNotificationToUser(Integer groupId, Integer userId, String groupName) {
        logger.info("Sending pending approval notification to user: {} for group: {}", userId, groupId);
        
        // Validate input first - these exceptions should be thrown
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("Group ID must be positive");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }
        
        try {
            // Get user to send notification to
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                logger.warn("User not found: {}", userId);
                return;
            }
            User user = userOpt.get();
            
            // Create notification for the user themselves (system notification)
            String title = "Join Request Pending";
            String message = String.format("Your request to join \"%s\" group is now pending approval", groupName);
            String linkToEntity = "seimaapp://groups/" + groupId + "/status";
            
            Notification notification = createNotification(user, null, groupId, 
                                                         NotificationType.GROUP_JOIN_REQUEST, title, 
                                                         message, linkToEntity);
            
            // Save notification
            Notification savedNotification = notificationRepository.save(notification);
            
            // Send FCM notification
            boolean fcmSuccess = sendFcmNotificationToUser(userId, title, message, groupId);
            
            // Update sent_at timestamp if FCM was successful
            if (fcmSuccess) {
                savedNotification.setSentAt(LocalDateTime.now());
                notificationRepository.save(savedNotification);
            }
            
            logger.info("Successfully sent pending approval notification to user: {} for group: {}", userId, groupId);
            
        } catch (Exception e) {
            logger.error("Error sending pending approval notification to user: {} for group: {}", userId, groupId, e);
            // Don't throw exception to avoid affecting main flow
        }
    }



    @Override
    @Transactional
    public void sendRoleUpdateNotificationToUser(Integer groupId, Integer userId, String groupName, 
                                               String updatedByUserName, GroupMemberRole previousRole, GroupMemberRole newRole) {
        logger.info("Sending role update notification to user {} for group {} - role changed from {} to {}", 
                   userId, groupId, previousRole, newRole);
        
        // Validate inputs
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("Group ID must be positive");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }
        if (updatedByUserName == null || updatedByUserName.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by user name cannot be empty");
        }
        if (previousRole == null) {
            throw new IllegalArgumentException("Previous role cannot be null");
        }
        if (newRole == null) {
            throw new IllegalArgumentException("New role cannot be null");
        }
        
        try {
            // Get user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
            
            // Get group
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));
            
            // Create notification
            String title = "Role Updated";
            String message = String.format("Your role in '%s' has been updated from %s to %s by %s", 
                                         groupName, previousRole, newRole, updatedByUserName);
            String linkToEntity = String.format("/groups/%d", groupId);
            
            Notification notification = createNotification(user, null, groupId, 
                                                        NotificationType.GROUP_ROLE_UPDATED, title, message, linkToEntity);
            
            // Save notification
            notificationRepository.save(notification);
            
            // Send FCM notification
            sendFcmNotificationToUser(userId, title, message, groupId);
            
            logger.info("Successfully sent role update notification to user {} for group {}", userId, groupId);
            
        } catch (Exception e) {
            logger.error("Failed to send role update notification to user {} for group {}", userId, groupId, e);
            // Don't throw exception to avoid affecting main flow
        }
    }

    /**
     * Process notifications asynchronously to avoid blocking main thread
     */
    @Async("notificationTaskExecutor")
    public CompletableFuture<Void> processNotificationsAsync(List<GroupMember> adminAndOwnerMembers, 
                                                            User senderUser, Integer groupId, 
                                                            NotificationType notificationType, String title, 
                                                            String message, String linkToEntity, String senderUserName) {
        logger.info("Processing {} notifications asynchronously", adminAndOwnerMembers.size());
        
        try {
            // Save notifications in batch
            List<Notification> notifications = saveNotificationsBatch(adminAndOwnerMembers, senderUser, 
                                                                     groupId, notificationType, title, 
                                                                     message, linkToEntity);
            
            // Send FCM notifications
            List<Integer> userIds = adminAndOwnerMembers.stream()
                .map(member -> member.getUser().getUserId())
                .collect(Collectors.toList());
            
            boolean fcmSuccess = sendFcmNotifications(userIds, senderUserName, groupId, senderUser.getUserId(), 
                                                    title, message);
            
            // Update sent_at timestamp if FCM was successful
            if (fcmSuccess && !notifications.isEmpty()) {
                updateNotificationsSentAt(notifications);
            }
            
            logger.info("Successfully processed {} notifications", notifications.size());
            
        } catch (Exception e) {
            logger.error("Error processing notifications asynchronously", e);
            // Don't rethrow - this is async and shouldn't affect main flow
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Validate generic notification input
     */
    private void validateGenericNotificationInput(Integer groupId, Integer senderUserId, String senderUserName, 
                                                 NotificationType notificationType, String title, String message) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("Group ID must be positive");
        }
        if (senderUserId == null || senderUserId <= 0) {
            throw new IllegalArgumentException("Sender user ID must be positive");
        }
        if (senderUserName == null || senderUserName.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender user name cannot be empty");
        }
        if (notificationType == null) {
            throw new IllegalArgumentException("Notification type cannot be null");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
    }
    
    /**
     * Save notifications in batch for better performance
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Notification> saveNotificationsBatch(List<GroupMember> adminAndOwnerMembers, 
                                                     User senderUser, Integer groupId, 
                                                     NotificationType notificationType, String title, 
                                                     String message, String linkToEntity) {
        List<Notification> notifications = new ArrayList<>();
        
        try {
            // Process in batches to avoid memory issues
            for (int i = 0; i < adminAndOwnerMembers.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, adminAndOwnerMembers.size());
                List<GroupMember> batch = adminAndOwnerMembers.subList(i, end);
                
                List<Notification> batchNotifications = batch.stream()
                    .map(member -> createNotification(member.getUser(), senderUser, groupId, 
                                                     notificationType, title, message, linkToEntity))
                    .collect(Collectors.toList());
                
                // Save batch
                List<Notification> savedNotifications = notificationRepository.saveAll(batchNotifications);
                notifications.addAll(savedNotifications);
                
                // Update cache for each receiver
                for (Notification notification : savedNotifications) {
                    Integer receiverId = notification.getReceiver().getUserId();
                    try {
                        notificationCacheService.incrementUnreadCount(receiverId);
                    } catch (Exception e) {
                        logger.warn("Failed to increment cache for user {}: {}", receiverId, e.getMessage());
                    }
                }
                
                logger.debug("Saved batch of {} notifications", savedNotifications.size());
            }
            
            logger.info("Successfully saved {} notifications to database", notifications.size());
            
        } catch (Exception e) {
            logger.error("Error saving notifications batch", e);
            throw e;
        }
        
        return notifications;
    }
    
    /**
     * Create a notification object
     */
    private Notification createNotification(User receiver, User sender, Integer groupId, 
                                          NotificationType notificationType, String title, 
                                          String message, String linkToEntity) {
        Notification notification = new Notification();
        notification.setSender(sender);
        notification.setReceiver(receiver);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLinkToEntity(linkToEntity);
        notification.setIsRead(false);
        
        return notification;
    }
    
    /**
     * Send FCM notifications with error handling
     * @return true if FCM notifications were sent successfully, false otherwise
     */
    private boolean sendFcmNotifications(List<Integer> userIds, String senderUserName, 
                                       Integer groupId, Integer senderUserId, String title, String message) {
        logger.info("Sending FCM notifications to {} users", userIds.size());
        
        try {
            // Get FCM tokens
            List<String> fcmTokens = userDeviceRepository.findFcmTokensByUserIds(userIds);
            
            if (fcmTokens.isEmpty()) {
                logger.warn("No FCM tokens found for users: {}", userIds);
                return false;
            }
            
            // Prepare notification data
            Map<String, String> data = Map.of(
                "type", "group_notification",
                "groupId", groupId.toString(),
                "senderUserId", senderUserId.toString(),
                "senderUserName", senderUserName
            );
            
            // Send FCM notifications
            fcmService.sendMulticastNotification(fcmTokens, title, message, data);
            
            logger.info("Successfully sent FCM notifications to {} tokens", fcmTokens.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error sending FCM notifications", e);
            return false;
        }
    }
    

    private boolean sendFcmNotificationToUser(Integer userId, String title, String message, Integer groupId) {
        logger.info("Sending FCM notification to user: {}", userId);
        
        try {
            // Get FCM tokens for the specific user
            List<String> fcmTokens = userDeviceRepository.findFcmTokensByUserIds(List.of(userId));
            
            if (fcmTokens.isEmpty()) {
                logger.warn("No FCM tokens found for user: {}", userId);
                return false;
            }
            
            // Prepare notification data
            Map<String, String> data = Map.of(
                "type", "group_notification",
                "groupId", groupId.toString(),
                "notificationType", "PENDING_APPROVAL"
            );
            
            // Send FCM notification
            fcmService.sendMulticastNotification(fcmTokens, title, message, data);
            
            logger.info("Successfully sent FCM notification to user: {} with {} tokens", userId, fcmTokens.size());
            return true;
            
        } catch (Exception e) {
            logger.error("Error sending FCM notification to user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Update sent_at timestamp for notifications after successful FCM delivery
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateNotificationsSentAt(List<Notification> notifications) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (Notification notification : notifications) {
                notification.setSentAt(now);
            }
            
            notificationRepository.saveAll(notifications);
            
            logger.info("Updated sent_at timestamp for {} notifications", notifications.size());
            
        } catch (Exception e) {
            logger.error("Error updating notifications sent_at timestamp", e);
            // Don't rethrow - this is not critical for main flow
        }
    }

    @Override
    @Transactional
    public void sendRoleUpdateNotificationToGroup(Integer groupId, Integer updatedUserId, String updatedUserName, 
                                                String updatedByUserName, GroupMemberRole previousRole, GroupMemberRole newRole) {
        logger.info("Sending role update notification to group: {} for user: {} by user: {}", 
                   groupId, updatedUserId, updatedByUserName);
        
        // Validate inputs
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("Group ID must be positive");
        }
        if (updatedUserId == null || updatedUserId <= 0) {
            throw new IllegalArgumentException("Updated user ID must be positive");
        }
        if (updatedUserName == null || updatedUserName.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated user name cannot be empty");
        }
        if (updatedByUserName == null || updatedByUserName.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by user name cannot be empty");
        }
        if (previousRole == null) {
            throw new IllegalArgumentException("Previous role cannot be null");
        }
        if (newRole == null) {
            throw new IllegalArgumentException("New role cannot be null");
        }
        
        try {
            // Get all active members of the group (excluding the updated user)
            List<GroupMember> groupMembers = groupMemberRepository.findByGroupAndStatusAndUserIdNot(
                    groupId, GroupMemberStatus.ACTIVE, updatedUserId);
            
            if (groupMembers.isEmpty()) {
                logger.warn("No active members found in group: {} (excluding updated user: {})", groupId, updatedUserId);
                return;
            }
            
            // Create notification content
            String title = "Role Updated";
            String message = String.format("%s's role has been changed from %s to %s by %s", 
                    updatedUserName, previousRole, newRole, updatedByUserName);
            String linkToEntity = String.format("/groups/%d/members", groupId);
            
            // Send notifications to all group members
            sendNotificationToGroupMembers(groupId, updatedUserId, updatedByUserName, 
                                        NotificationType.GROUP_ROLE_UPDATED, title, message, linkToEntity);
            
            logger.info("Successfully sent role update notification to {} members in group: {}", 
                       groupMembers.size(), groupId);
            
        } catch (Exception e) {
            logger.error("Error sending role update notification to group: {} for user: {}", 
                        groupId, updatedUserId, e);
            // Don't throw exception to avoid affecting main flow
        }
    }

    @Override
    @Transactional
    public void sendMemberRemovedNotificationToGroup(Integer groupId, Integer removedUserId, String removedUserName, String removedByUserName) {
        logger.info("Sending member removed notification to group: {} for user: {} by user: {}", groupId, removedUserId, removedByUserName);
        try {
            // Get all active members of the group (excluding the removed user)
            List<GroupMember> groupMembers = groupMemberRepository.findByGroupAndStatusAndUserIdNot(
                    groupId, GroupMemberStatus.ACTIVE, removedUserId);
            if (groupMembers.isEmpty()) {
                logger.warn("No active members found in group: {} (excluding removed user: {})", groupId, removedUserId);
                return;
            }
            String title = "Member Removed";
            String message = String.format("%s has been removed from the group by %s", removedUserName, removedByUserName);
            String linkToEntity = String.format("/groups/%d/members", groupId);
            sendNotificationToGroupMembers(groupId, removedUserId, removedByUserName, NotificationType.GROUP_MEMBER_REMOVED, title, message, linkToEntity);
            logger.info("Successfully sent member removed notification to {} members in group: {}", groupMembers.size(), groupId);
        } catch (Exception e) {
            logger.error("Error sending member removed notification to group: {} for user: {}", groupId, removedUserId, e);
        }
    }

    @Override
    @Transactional
    public void sendNotificationToGroupMembersExceptUser(Integer groupId, Integer senderUserId, String senderUserName, 
                                                        NotificationType notificationType, String title, String message, 
                                                        String linkToEntity) {
        logger.info("Sending notification to group members except user for group: {}, user: {}, type: {}", 
                   groupId, senderUserId, notificationType);
        
        try {
            // Validate input
            validateGenericNotificationInput(groupId, senderUserId, senderUserName, notificationType, title, message);
            
            // Get all active group members except the sender user
            List<GroupMember> activeMembers = groupMemberRepository.findByGroupAndStatusAndUserIdNot(
                groupId, GroupMemberStatus.ACTIVE, senderUserId);
            
            if (activeMembers.isEmpty()) {
                logger.warn("No active members found for group: {} (excluding sender user: {})", groupId, senderUserId);
                return;
            }
            
            // Get sender user
            Optional<User> senderUserOpt = userRepository.findById(senderUserId);
            if (senderUserOpt.isEmpty()) {
                logger.warn("Sender user not found: {}", senderUserId);
                return;
            }
            User senderUser = senderUserOpt.get();
            
            // Process notifications asynchronously for better performance
            processNotificationsAsync(activeMembers, senderUser, groupId, notificationType, 
                                    title, message, linkToEntity, senderUserName);
            
            logger.info("Successfully initiated notification to {} group members (excluding sender user)", 
                activeMembers.size());
            
        } catch (Exception e) {
            logger.error("Error sending notification for group: {}, user: {}, type: {}", 
                groupId, senderUserId, notificationType, e);
            throw e;
        }
    }
} 