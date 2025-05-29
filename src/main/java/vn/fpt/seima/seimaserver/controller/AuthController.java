package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.auth.OtpRequestDto;
import vn.fpt.seima.seimaserver.service.OTPService;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final OTPService otpService;
    @PostMapping("otp/request")
    public ResponseEntity<ApiResponse<Object>> requestOtp(
            @Valid
            @RequestBody OtpRequestDto otpRequestDto
    )
    {
        otpService.generateOtpAndSendOtp(otpRequestDto.getPhoneNumber());
        return ResponseEntity.ok(response);
    }
}
