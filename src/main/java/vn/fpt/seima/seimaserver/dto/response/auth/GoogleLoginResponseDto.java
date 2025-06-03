package vn.fpt.seima.seimaserver.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.fpt.seima.seimaserver.dto.response.user.UserInGoogleReponseDto;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private UserInGoogleReponseDto userInfomation;
    private Boolean isFirstLogin;

}
