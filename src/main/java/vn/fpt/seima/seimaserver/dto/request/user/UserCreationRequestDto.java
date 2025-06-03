package vn.fpt.seima.seimaserver.dto.request.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreationRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;


    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(0[3|5|7|8|9])([0-9]{8})$", message = "Invalid Vietnamese phone number format")
    @Size(min=10, max = 11, message = "Phone number must be 10 or 11 digits")
    private String phoneNumber;
    private String avatarUrl;

    private boolean gender;
}
