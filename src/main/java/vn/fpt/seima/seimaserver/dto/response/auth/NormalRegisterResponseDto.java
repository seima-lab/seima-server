package vn.fpt.seima.seimaserver.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NormalRegisterResponseDto {
    private String fullName;
    private String email;
    private LocalDate dob;
    private String phoneNumber;
    private boolean gender;
    private String password;
    private String otpCode;
}
