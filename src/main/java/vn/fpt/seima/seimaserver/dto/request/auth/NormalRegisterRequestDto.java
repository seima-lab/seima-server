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
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&.]{8,}$",
        message = "Password must be at least 8 characters long and contain at least one letter and one number"
    )
    private String password;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
