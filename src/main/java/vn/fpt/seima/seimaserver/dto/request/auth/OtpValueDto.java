package vn.fpt.seima.seimaserver.dto.request.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OtpValueDto {
    private String otpCode;
    private int attemptCount;
    private Integer incorrectAttempts;
}
