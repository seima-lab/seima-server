package vn.fpt.seima.seimaserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.auth.GoogleLoginRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ForgotPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.LoginRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.LogoutRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.SetNewPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyForgotPasswordOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.GoogleLoginResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.LoginResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.NormalRegisterResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.VerifyForgotPasswordOtpResponseDto;
import vn.fpt.seima.seimaserver.dto.response.user.UserInGoogleReponseDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.exception.GoogleAccountConflictException;
import vn.fpt.seima.seimaserver.exception.InvalidOtpException;
import vn.fpt.seima.seimaserver.exception.MaxOtpAttemptsExceededException;
import vn.fpt.seima.seimaserver.exception.NullRequestParamException;
import vn.fpt.seima.seimaserver.exception.OtpNotFoundException;
import vn.fpt.seima.seimaserver.exception.PasswordMismatchException;
import vn.fpt.seima.seimaserver.exception.InvalidPasswordException;
import vn.fpt.seima.seimaserver.exception.AccountNotVerifiedException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private GoogleService googleService;
    private JwtService jwtService;
    private UserRepository userRepository;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private AuthService authService;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    UserDeviceService userDeviceService;

    // Google Login
    @PostMapping("/google")
    public ApiResponse<Object> googleLogin(
            @Valid
            @RequestBody GoogleLoginRequestDto googleLoginRequestDto
            ) {
        try {
            System.out.println("🟢 Controller: /api/v1/auth/google called");
            String IdToken= googleLoginRequestDto.getIdToken();
            if (IdToken == null) {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .message("IdToken is null")
                        .build();
            }
            GoogleLoginResponseDto data = googleService.processGoogleLogin(IdToken);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Google login successful")
                    .data(data)
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh token is missing or malformed"));
        }

        String refreshToken = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(refreshToken)) { // Basic validation: not expired, signature ok
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
            }

            String email = jwtService.extractEmail(refreshToken);
            // It's crucial to load UserDetails to ensure the user still exists and is active
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            User appUser = userRepository.findByUserEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found during refresh token validation"));



            UserInGoogleReponseDto userInGoogleReponseDto = UserInGoogleReponseDto.
                    builder()
                    .email(appUser.getUserEmail())
                    .fullName(appUser.getUserFullName())
                    .avatarUrl(appUser.getUserAvatarUrl())
                    .build();
            String newAccessToken = jwtService.generateAccessToken(userInGoogleReponseDto);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (Exception e) {
            // Log the exception
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ApiResponse<Object> register(
            @Valid
            @RequestBody NormalRegisterRequestDto normalRegisterRequestDto
    ) {
        try {

           NormalRegisterResponseDto otp = authService.processRegister(normalRegisterRequestDto);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Registration successful")
                    .data(otp)
                    .build();
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        }catch (GmailAlreadyExistException e){
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        }catch(InvalidOtpException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message(e.getMessage())
                    .build();
        } catch (MaxOtpAttemptsExceededException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message(e.getMessage())
                    .build();
        }
        catch (Exception e) {
            logger.error("Error during registration: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Registration failed: " + e.getMessage())
                    .build();
        }
    }
    @PostMapping("/verify-otp")
    public ApiResponse<Object> verifyOtp(
            @Valid
            @RequestBody VerifyOtpRequestDto verifyOtpRequestDto
    ) {
        try {
            boolean verified = authService.verifyOtp(verifyOtpRequestDto);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("OTP verification successful")
                    .data(verified)
                    .build();
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (OtpNotFoundException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message(e.getMessage())
                    .build();
        } catch (InvalidOtpException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message(e.getMessage())
                    .build();
        } catch (MaxOtpAttemptsExceededException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during OTP verification: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("OTP verification failed: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Object> resendOtp(
            @RequestBody Map<String, String> request
    ) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Email is required")
                        .build();
            }
            
            authService.resendOtp(email);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("OTP resent successfully")
                    .build();
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (GmailAlreadyExistException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        } catch (MaxOtpAttemptsExceededException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during OTP resend: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("OTP resend failed: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/login")
    public ApiResponse<Object> login(
            @Valid
            @RequestBody LoginRequestDto loginRequestDto
    ) {
        try {
            LoginResponseDto loginResponse = authService.login(loginRequestDto);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Login successful")
                    .data(loginResponse)
                    .build();
        } catch (AccountNotVerifiedException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .message(e.getMessage())
                    .build();
        } catch (InvalidOtpException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during login: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Login failed: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @Valid @RequestBody LogoutRequestDto logoutRequestDto,
            HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                
                // Get token expiration time
                Date expiration = jwtService.extractExpiration(jwt);
                long expirationTime = expiration.getTime();
                
                // Blacklist the token
                tokenBlacklistService.blacklistToken(jwt, expirationTime);
                
                // Update device to set user ID to null
                userDeviceService.updateUserIdToNull(logoutRequestDto.getDeviceId());
                
                return ResponseEntity.ok(ApiResponse.<String>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Logout successful")
                        .data("User logged out successfully")
                        .build());
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("No valid token found")
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<String>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Logout failed: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Object> forgotPassword(
            @Valid
            @RequestBody ForgotPasswordRequestDto forgotPasswordRequestDto
    ) {
        try {
            authService.forgotPassword(forgotPasswordRequestDto);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Password reset OTP sent successfully")
                    .build();
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during forgot password: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to send password reset OTP: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/resend-forgot-password-otp")
    public ApiResponse<Object> resendForgotPasswordOtp(
            @RequestBody Map<String, String> request
    ) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Email is required")
                        .build();
            }
            
            authService.resendForgotPasswordOtp(email);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Password reset OTP resent successfully")
                    .build();
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (GoogleAccountConflictException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        } catch (MaxOtpAttemptsExceededException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during forgot password OTP resend: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to resend password reset OTP: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/verify-forgot-password-otp")
    public ApiResponse<Object> verifyForgotPasswordOtp(
            @Valid @RequestBody VerifyForgotPasswordOtpRequestDto verifyForgotPasswordOtpRequestDto
    ) {
        try {
            VerifyForgotPasswordOtpResponseDto result = authService.verifyForgotPasswordOtp(verifyForgotPasswordOtpRequestDto);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("OTP verification successful")
                    .data(result)
                    .build();
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (GoogleAccountConflictException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        } catch (OtpNotFoundException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message(e.getMessage())
                    .build();
        } catch (InvalidOtpException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message(e.getMessage())
                    .build();
        } catch (MaxOtpAttemptsExceededException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during forgot password OTP verification: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("OTP verification failed: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/set-new-password-after-verification")
    public ApiResponse<Object> setNewPasswordAfterVerification(
            @Valid @RequestBody SetNewPasswordRequestDto setNewPasswordRequestDto
    ) {
        try {
            boolean result = authService.setNewPasswordAfterVerification(setNewPasswordRequestDto, setNewPasswordRequestDto.getVerificationToken());
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Password reset successful")
                    .data(result)
                    .build();
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (GoogleAccountConflictException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during password reset: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Password reset failed: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/change-password")
    public ApiResponse<Object> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto changePasswordRequestDto,
            HttpServletRequest request
    ) {
        try {
            // Get current user email from JWT token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .message("Authorization token is required")
                        .build();
            }
            
            String token = authHeader.substring(7);
            String userEmail = jwtService.extractEmail(token);
            
            boolean result = authService.changePassword(userEmail, changePasswordRequestDto);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Password changed successfully")
                    .data(result)
                    .build();
                    
        } catch (NullRequestParamException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (PasswordMismatchException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (InvalidPasswordException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                    .message(e.getMessage())
                    .build();
        } catch (GoogleAccountConflictException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .message(e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during password change: ", e);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Password change failed: " + e.getMessage())
                    .build();
        }
    }
}
