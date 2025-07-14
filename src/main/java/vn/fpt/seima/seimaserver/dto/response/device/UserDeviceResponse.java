package vn.fpt.seima.seimaserver.dto.response.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDeviceResponse {
    
    private Integer id;
    private Integer userId;
    private String deviceId;
    private String fcmToken;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 