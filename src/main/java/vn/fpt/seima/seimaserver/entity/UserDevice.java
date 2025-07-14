package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_device", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_device_device_id", columnNames = "device_id")
       })
public class UserDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "device_id", length = 255, nullable = false, unique = true)
    private String deviceId;
    
    @Column(name = "fcm_token", length = 500, nullable = false)
    private String fcmToken;
    
    @Column(name = "last_change")
    private LocalDateTime lastChange;
    
    // Relationship với User (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
} 