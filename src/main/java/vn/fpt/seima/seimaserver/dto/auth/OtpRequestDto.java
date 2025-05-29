package vn.fpt.seima.seimaserver.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestDto {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(0[3|5|7|8|9])([0-9]{8})$", message = "Invalid Vietnamese phone number format")
    @Size(min=10, max = 11, message = "Phone number must be 10 or 11 digits")
    private String phoneNumber;
}
