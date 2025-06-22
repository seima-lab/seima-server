package vn.fpt.seima.seimaserver.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyForgotPasswordOtpResponseDto {
    private String email;
    private boolean verified;
    private String verificationToken; // Token để xác thực việc user đã verify OTP thành công
    private long expiresIn; // Token expiration time in seconds
} 