package vn.fpt.seima.seimaserver.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInGoogleReponseDto {
    private String email;
    private String fullName;
    private String avatarUrl;
}
