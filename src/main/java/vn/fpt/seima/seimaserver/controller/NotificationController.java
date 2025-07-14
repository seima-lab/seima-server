package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.response.notification.NotificationResponse;
import vn.fpt.seima.seimaserver.entity.NotificationType;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.service.NotificationService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    

    /**
     * Get notifications with pagination and filtering
     * @param page page number (default: 0)
     * @param size page size (default: 10)
     * @param isRead filter by read status (null = all)
     * @param type filter by notification type (null = all)
     * @param startDate filter by start date (null = no start limit)
     * @param endDate filter by end date (null = no end limit)
     * @return ApiResponse containing paginated notifications
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Page<NotificationResponse>> getNotificationsWithFilters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        // Get current authenticated user
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.<Page<NotificationResponse>>builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated")
                    .build();
        }
        
        try {
            // Create pageable
            Pageable pageable = PageRequest.of(page, size);
            
            // Fetch filtered notifications
            Page<NotificationResponse> notifications = notificationService.getNotificationsWithFilters(
                    currentUser.getUserId(), isRead, type, startDate, endDate, pageable);
            
            return ApiResponse.<Page<NotificationResponse>>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Notifications fetched successfully")
                    .data(notifications)
                    .build();
        } catch (IllegalArgumentException ex) {
            return ApiResponse.<Page<NotificationResponse>>builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(ex.getMessage())
                    .build();
        } catch (Exception ex) {
            return ApiResponse.<Page<NotificationResponse>>builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("An unexpected error occurred")
                    .build();
        }
    }
    

    /**
     * Get unread notification count (for badge)
     * @return ApiResponse containing unread count
     */
    @GetMapping("/unread-count")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Long> getUnreadNotificationCount() {
        // Get current authenticated user
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.<Long>builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated")
                    .build();
        }
        
        try {
            long unreadCount = notificationService.getUnreadNotificationCount(currentUser.getUserId());
            
            return ApiResponse.<Long>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Unread notification count fetched successfully")
                    .data(unreadCount)
                    .build();
        } catch (Exception ex) {
            return ApiResponse.<Long>builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("An unexpected error occurred")
                    .build();
        }
    }
    

    @PutMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> markNotificationAsRead(@PathVariable Integer notificationId) {
        // Get current authenticated user
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated")
                    .build();
        }
        
        // Mark notification as read
        boolean success = notificationService.markNotificationAsRead(notificationId, currentUser.getUserId());
        
        if (success) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Notification marked as read successfully")
                    .build();
        } else {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("Notification not found or already read")
                    .build();
        }
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Integer> markAllNotificationsAsRead() {
        // Get current authenticated user
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.<Integer>builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated")
                    .build();
        }
        
        // Mark all notifications as read
        int markedCount = notificationService.markAllNotificationsAsRead(currentUser.getUserId());
        
        return ApiResponse.<Integer>builder()
                .statusCode(HttpStatus.OK.value())
                .message(String.format("Successfully marked %d notifications as read", markedCount))
                .data(markedCount)
                .build();
    }
    
    /**
     * Delete a specific notification
     * @param notificationId ID of the notification to delete
     * @return ApiResponse indicating success or failure
     */
    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> deleteNotification(@PathVariable Integer notificationId) {
        // Get current authenticated user
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated")
                    .build();
        }
        
        try {
            boolean success = notificationService.deleteNotification(notificationId, currentUser.getUserId());
            
            if (success) {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Notification deleted successfully")
                        .build();
            } else {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .message("Notification not found")
                        .build();
            }
        } catch (IllegalArgumentException ex) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(ex.getMessage())
                    .build();
        } catch (Exception ex) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("An unexpected error occurred")
                    .build();
        }
    }
    
    /**
     * Delete all notifications for the current user
     * @return ApiResponse with count of deleted notifications
     */
    @DeleteMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Integer> deleteAllNotifications() {
        // Get current authenticated user
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.<Integer>builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message("User not authenticated")
                    .build();
        }
        
        try {
            int deletedCount = notificationService.deleteAllNotifications(currentUser.getUserId());
            
            return ApiResponse.<Integer>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message(String.format("Successfully deleted %d notifications", deletedCount))
                    .data(deletedCount)
                    .build();
        } catch (Exception ex) {
            return ApiResponse.<Integer>builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("An unexpected error occurred")
                    .build();
        }
    }
    

} 