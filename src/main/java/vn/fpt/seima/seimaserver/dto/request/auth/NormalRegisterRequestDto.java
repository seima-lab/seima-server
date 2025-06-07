package vn.fpt.seima.seimaserver.dto.request.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NormalRegisterRequestDto {
    private String fullName;
    private String email;
    private LocalDate dob;
    private String phoneNumber;
    private boolean gender;
    private String password;
    private String confirmPassword;
}
