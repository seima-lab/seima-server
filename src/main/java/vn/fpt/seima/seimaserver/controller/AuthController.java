package vn.fpt.seima.seimaserver.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.auth.OtpRequestDto;
import vn.fpt.seima.seimaserver.exception.PhoneNumberAlreadyExistsException;
import vn.fpt.seima.seimaserver.exception.RateLimitExceededException;
import vn.fpt.seima.seimaserver.service.OTPService;

import java.util.stream.Collectors;

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
    ) {
        try {
            logger.info("Received OTP request for phone number: {}", otpRequestDto.getPhoneNumber());
            otpService.generateOtpAndSendOtp(otpRequestDto.getPhoneNumber());
            return ResponseEntity.ok(new ApiResponse<>(true, "OTP has been sent successfully. Please check your phone."));
        } catch (RateLimitExceededException e) {
            logger.warn("Rate limit exceeded. Sending OTP request again.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>(false, e.getMessage()));
        } catch (PhoneNumberAlreadyExistsException e) {
            logger.warn("Phone number already exists: {}", otpRequestDto.getPhoneNumber());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(false, e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("An unexpected error occurred while processing OTP request for phone number: {}", otpRequestDto.getPhoneNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "An unexpected error occurred. Please try again later."));
        }
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("Validation error: {}", errorMessage);
        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>(false, "Validation failed: " + errorMessage));
    }
}