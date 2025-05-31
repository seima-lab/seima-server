package vn.fpt.seima.seimaserver.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRedisValueDto {
    private String otp;
    private Integer attempts;
}
