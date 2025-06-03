package vn.fpt.seima.seimaserver.dto.request.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInGoogleReponseDto {
    private String email;
    private String fullName;
    private String avatarUrl;
}
