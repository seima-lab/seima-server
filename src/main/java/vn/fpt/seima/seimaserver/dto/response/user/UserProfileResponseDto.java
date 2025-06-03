package vn.fpt.seima.seimaserver.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {
    private Integer userId;
    private String userFullName;
    private String userEmail;
    private LocalDate userDob;
    private Boolean userGender;
    private String userPhoneNumber;
    private String userAvatarUrl;
}
