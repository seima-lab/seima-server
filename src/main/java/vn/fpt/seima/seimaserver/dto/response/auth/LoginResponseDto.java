package vn.fpt.seima.seimaserver.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.dto.response.user.UserInGoogleReponseDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private UserInGoogleReponseDto userInformation;
    private String message;
} 