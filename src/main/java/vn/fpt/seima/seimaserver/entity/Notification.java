package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_notification_receiver", columnList = "receiver_id"),
    @Index(name = "idx_notification_sender", columnList = "sender_id"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_created", columnList = "created_at"),
    @Index(name = "idx_notification_receiver_type_created", columnList = "receiver_id, notification_type, created_at")
})
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = true, 
                foreignKey = @ForeignKey(name = "fk_notification_sender"))
    private User sender; // User who triggered the notification (nullable for system notifications)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notification_receiver"))
    private User receiver; // User who receives the notification

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50, nullable = false)
    private NotificationType notificationType;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "link_to_entity", length = 255)
    private String linkToEntity;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at", nullable = true)
    private LocalDateTime sentAt; // Thời điểm notification được gửi đi (FCM, email, etc.)
}
