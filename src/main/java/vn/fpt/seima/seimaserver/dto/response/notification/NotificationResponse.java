package vn.fpt.seima.seimaserver.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.entity.NotificationType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    
    private Integer notificationId;
    private String title;
    private String message;
    private NotificationType notificationType;
    private String linkToEntity;
    private Boolean isRead;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;
    
    // Sender information (null for system notifications)
    private SenderInfo sender;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SenderInfo {
        private Integer userId;
        private String userFullName;
        private String userAvatarUrl;
    }
} 