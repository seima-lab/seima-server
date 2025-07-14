package vn.fpt.seima.seimaserver.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequestDto {
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
} 