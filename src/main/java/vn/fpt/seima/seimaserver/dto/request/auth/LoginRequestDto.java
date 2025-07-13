package vn.fpt.seima.seimaserver.dto.request.auth;

import jakarta.validation.constraints.Email;
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
public class LoginRequestDto {
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    private String deviceId;

    @NotBlank(message = "FCM token is required")
    @Size(max = 500, message = "FCM token must not exceed 500 characters")
    private String fcmToken;

}