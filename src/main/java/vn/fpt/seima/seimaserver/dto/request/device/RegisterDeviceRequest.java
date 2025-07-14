package vn.fpt.seima.seimaserver.dto.request.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDeviceRequest {
    
    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    private String deviceId;
    
    @NotBlank(message = "FCM token is required")
    @Size(max = 500, message = "FCM token must not exceed 500 characters")
    private String fcmToken;
} 