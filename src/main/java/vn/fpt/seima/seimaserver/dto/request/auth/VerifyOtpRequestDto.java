package vn.fpt.seima.seimaserver.dto.request.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequestDto {
    private String email;
    private String fullName;
    private LocalDate dob;
    private String phoneNumber;
    private boolean gender;
    private String password;
    private String otp;
} 