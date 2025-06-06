package vn.fpt.seima.seimaserver.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NormalRegisterResponseDto {
    private String email;
    private String otpCode;
}
