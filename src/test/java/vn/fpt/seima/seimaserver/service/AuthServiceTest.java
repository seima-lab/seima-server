package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ForgotPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.OtpValueDto;
import vn.fpt.seima.seimaserver.dto.request.auth.SetNewPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyForgotPasswordOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.VerifyForgotPasswordOtpResponseDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GoogleAccountConflictException;
import vn.fpt.seima.seimaserver.exception.InvalidOtpException;
import vn.fpt.seima.seimaserver.exception.MaxOtpAttemptsExceededException;
import vn.fpt.seima.seimaserver.exception.NullRequestParamException;
import vn.fpt.seima.seimaserver.exception.OtpNotFoundException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.impl.AuthServiceImpl;
import vn.fpt.seima.seimaserver.util.OtpUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RedisService redisService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidationService passwordValidationService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    // Test data for forgot password functionality
    private ForgotPasswordRequestDto validForgotPasswordRequest;
    private VerifyForgotPasswordOtpRequestDto validVerifyOtpRequest;
    private SetNewPasswordRequestDto validSetPasswordRequest;
    private ChangePasswordRequestDto validChangePasswordRequest;
    private User normalUser;
    private User googleUser;
    private OtpValueDto validOtpValue;
    private String testEmail;
    private String testOtp;
    private String testVerificationToken;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testOtp = "123456";
        testVerificationToken = "verification-token-123";

        // Setup forgot password request
        validForgotPasswordRequest = ForgotPasswordRequestDto.builder()
                .email(testEmail)
                .build();

        // Setup verify OTP request
        validVerifyOtpRequest = VerifyForgotPasswordOtpRequestDto.builder()
                .email(testEmail)
                .otp(testOtp)
                .build();

        // Setup set new password request
        validSetPasswordRequest = SetNewPasswordRequestDto.builder()
                .email(testEmail)
                .newPassword("NewPassword123")
                .verificationToken(testVerificationToken)
                .build();

        // Setup change password request
        validChangePasswordRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword("NewPassword123")
                .confirmNewPassword("NewPassword123")
                .build();

        // Setup normal user
        normalUser = User.builder()
                .userId(1)
                .userEmail(testEmail)
                .userFullName("Test User")
                .userPassword("encodedPassword")
                .isLogByGoogle(false)
                .userIsActive(true)
                .userCreatedDate(LocalDateTime.now())
                .build();

        // Setup Google user
        googleUser = User.builder()
                .userId(2)
                .userEmail("google@example.com")
                .userFullName("Google User")
                .userPassword(null)
                .isLogByGoogle(true)
                .userIsActive(true)
                .userCreatedDate(LocalDateTime.now())
                .build();

        // Setup OTP value
        validOtpValue = OtpValueDto.builder()
                .otpCode(testOtp)
                .attemptCount(1)
                .incorrectAttempts(0)
                .build();

        // Set up reflection test utils for constants
        ReflectionTestUtils.setField(authService, "labName", "Test Lab");
        ReflectionTestUtils.setField(authService, "passwordResetHtmlTemplate", "password-reset");
        ReflectionTestUtils.setField(authService, "passwordResetSubject", "Password Reset OTP");
    }

    // ===== FORGOT PASSWORD TESTS =====

    @Test
    void forgotPassword_Success_SendsOtpToValidUser() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(null);
        doNothing().when(redisService).set(anyString(), any(OtpValueDto.class));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), anyLong());
        doNothing().when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any(Context.class));

        try (MockedStatic<OtpUtils> otpUtilsMock = mockStatic(OtpUtils.class)) {
            otpUtilsMock.when(() -> OtpUtils.generateOTP(6)).thenReturn(testOtp);

            // When
            authService.forgotPassword(validForgotPasswordRequest);

            // Then
            verify(userRepository).findByUserEmail(testEmail);
            verify(redisService).set(eq("forgot-password-otp:" + testEmail), any(OtpValueDto.class));
            verify(redisService).setTimeToLiveInMinutes(eq("forgot-password-otp:" + testEmail), eq(5L));
            verify(emailService).sendEmailWithHtmlTemplate(eq(testEmail), anyString(), anyString(), any(Context.class));
        }
    }

    @Test
    void forgotPassword_ThrowsException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.empty());

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.forgotPassword(validForgotPasswordRequest)
        );
        assertEquals("User with email " + testEmail + " not found", exception.getMessage());
        verify(userRepository).findByUserEmail(testEmail);
        verifyNoInteractions(redisService, emailService);
    }

    @Test
    void forgotPassword_ThrowsException_WhenUserIsGoogleAccount() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(googleUser));

        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> authService.forgotPassword(validForgotPasswordRequest)
        );
        assertEquals("This account was created with Google login. Password reset is not available for Google accounts. Please use Google login.", exception.getMessage());
        verify(userRepository).findByUserEmail(testEmail);
        verifyNoInteractions(redisService, emailService);
    }

    @Test
    void forgotPassword_ThrowsException_WhenUserHasNoPassword() {
        // Given
        User userWithoutPassword = User.builder()
                .userId(1)
                .userEmail(testEmail)
                .userFullName("Test User")
                .userPassword(null)
                .isLogByGoogle(false)
                .userIsActive(true)
                .build();
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(userWithoutPassword));

        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> authService.forgotPassword(validForgotPasswordRequest)
        );
        assertEquals("This account does not have a password set. Please use Google login.", exception.getMessage());
    }

    @Test
    void forgotPassword_ThrowsException_WhenMaxAttemptsExceeded() {
        // Given
        OtpValueDto existingOtp = OtpValueDto.builder()
                .otpCode("654321")
                .attemptCount(5)
                .incorrectAttempts(0)
                .build();
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(existingOtp);

        // When & Then
        MaxOtpAttemptsExceededException exception = assertThrows(
                MaxOtpAttemptsExceededException.class,
                () -> authService.forgotPassword(validForgotPasswordRequest)
        );
        assertEquals("Maximum OTP attempts exceeded. Please try again later.", exception.getMessage());
        verify(userRepository).findByUserEmail(testEmail);
        verify(redisService).getObject("forgot-password-otp:" + testEmail, OtpValueDto.class);
        verifyNoInteractions(emailService);
    }

    @Test
    void forgotPassword_ThrowsException_WhenEmailSendingFails() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(null);
        doNothing().when(redisService).set(anyString(), any(OtpValueDto.class));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), anyLong());
        doThrow(new RuntimeException("Email service error")).when(emailService)
                .sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any(Context.class));
        doNothing().when(redisService).delete(anyString());

        try (MockedStatic<OtpUtils> otpUtilsMock = mockStatic(OtpUtils.class)) {
            otpUtilsMock.when(() -> OtpUtils.generateOTP(6)).thenReturn(testOtp);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> authService.forgotPassword(validForgotPasswordRequest)
            );
            assertEquals("Failed to send OTP email", exception.getMessage());
            verify(redisService).delete("forgot-password-otp:" + testEmail);
        }
    }

    // ===== RESEND FORGOT PASSWORD OTP TESTS =====

    @Test
    void resendForgotPasswordOtp_Success_ResendsOtpToValidUser() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(null);
        doNothing().when(redisService).set(anyString(), any(OtpValueDto.class));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), anyLong());
        doNothing().when(emailService).sendEmailWithHtmlTemplate(anyString(), anyString(), anyString(), any(Context.class));

        try (MockedStatic<OtpUtils> otpUtilsMock = mockStatic(OtpUtils.class)) {
            otpUtilsMock.when(() -> OtpUtils.generateOTP(6)).thenReturn(testOtp);

            // When
            authService.resendForgotPasswordOtp(testEmail);

            // Then
            verify(userRepository).findByUserEmail(testEmail);
            verify(redisService).set(eq("forgot-password-otp:" + testEmail), any(OtpValueDto.class));
            verify(redisService).setTimeToLiveInMinutes(eq("forgot-password-otp:" + testEmail), eq(5L));
            verify(emailService).sendEmailWithHtmlTemplate(eq(testEmail), anyString(), anyString(), any(Context.class));
        }
    }

    @Test
    void resendForgotPasswordOtp_ThrowsException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.empty());

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.resendForgotPasswordOtp(testEmail)
        );
        assertEquals("User with email " + testEmail + " not found", exception.getMessage());
    }

    @Test
    void resendForgotPasswordOtp_ThrowsException_WhenUserIsGoogleAccount() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(googleUser));

        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> authService.resendForgotPasswordOtp(testEmail)
        );
        assertEquals("This account was created with Google login. Password reset is not available for Google accounts. Please use Google login.", exception.getMessage());
    }

    // ===== VERIFY FORGOT PASSWORD OTP TESTS =====

    @Test
    void verifyForgotPasswordOtp_Success_ReturnsVerificationToken() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(validOtpValue);
        when(verificationTokenService.generateVerificationToken(testEmail)).thenReturn(testVerificationToken);
        doNothing().when(redisService).delete("forgot-password-otp:" + testEmail);

        // When
        VerifyForgotPasswordOtpResponseDto result = authService.verifyForgotPasswordOtp(validVerifyOtpRequest);

        // Then
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertTrue(result.isVerified());
        assertEquals(testVerificationToken, result.getVerificationToken());
        assertEquals(900L, result.getExpiresIn()); // 15 minutes = 900 seconds
        
        verify(userRepository).findByUserEmail(testEmail);
        verify(redisService).getObject("forgot-password-otp:" + testEmail, OtpValueDto.class);
        verify(verificationTokenService).generateVerificationToken(testEmail);
        verify(redisService).delete("forgot-password-otp:" + testEmail);
    }

    @Test
    void verifyForgotPasswordOtp_ThrowsException_WhenEmailIsNull() {
        // Given
        VerifyForgotPasswordOtpRequestDto invalidRequest = VerifyForgotPasswordOtpRequestDto.builder()
                .email(null)
                .otp(testOtp)
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.verifyForgotPasswordOtp(invalidRequest)
        );
        assertEquals("Email and OTP are required", exception.getMessage());
    }

    @Test
    void verifyForgotPasswordOtp_ThrowsException_WhenOtpIsNull() {
        // Given
        VerifyForgotPasswordOtpRequestDto invalidRequest = VerifyForgotPasswordOtpRequestDto.builder()
                .email(testEmail)
                .otp(null)
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.verifyForgotPasswordOtp(invalidRequest)
        );
        assertEquals("Email and OTP are required", exception.getMessage());
    }

    @Test
    void verifyForgotPasswordOtp_ThrowsException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.empty());

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.verifyForgotPasswordOtp(validVerifyOtpRequest)
        );
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void verifyForgotPasswordOtp_ThrowsException_WhenUserIsGoogleAccount() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(googleUser));

        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> authService.verifyForgotPasswordOtp(validVerifyOtpRequest)
        );
        assertEquals("This account was created with Google login. Password reset is not available for Google accounts.", exception.getMessage());
    }

    @Test
    void verifyForgotPasswordOtp_ThrowsException_WhenOtpNotFound() {
        // Given
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(null);

        // When & Then
        OtpNotFoundException exception = assertThrows(
                OtpNotFoundException.class,
                () -> authService.verifyForgotPasswordOtp(validVerifyOtpRequest)
        );
        assertEquals("OTP not found or expired", exception.getMessage());
    }

    @Test
    void verifyForgotPasswordOtp_ThrowsException_WhenOtpIsIncorrect() {
        // Given
        OtpValueDto incorrectOtpValue = OtpValueDto.builder()
                .otpCode("654321")
                .attemptCount(1)
                .incorrectAttempts(0)
                .build();
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(incorrectOtpValue);
        doNothing().when(redisService).set(anyString(), any(OtpValueDto.class));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), anyLong());

        // When & Then
        InvalidOtpException exception = assertThrows(
                InvalidOtpException.class,
                () -> authService.verifyForgotPasswordOtp(validVerifyOtpRequest)
        );
        assertEquals("Invalid OTP. Attempts remaining: 2", exception.getMessage());
    }

    @Test
    void verifyForgotPasswordOtp_ThrowsException_WhenMaxIncorrectAttemptsReached() {
        // Given
        OtpValueDto maxAttemptsOtpValue = OtpValueDto.builder()
                .otpCode("654321")
                .attemptCount(1)
                .incorrectAttempts(2)
                .build();
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(redisService.getObject("forgot-password-otp:" + testEmail, OtpValueDto.class)).thenReturn(maxAttemptsOtpValue);
        doNothing().when(redisService).delete("forgot-password-otp:" + testEmail);

        // When & Then
        MaxOtpAttemptsExceededException exception = assertThrows(
                MaxOtpAttemptsExceededException.class,
                () -> authService.verifyForgotPasswordOtp(validVerifyOtpRequest)
        );
        assertEquals("Maximum OTP verification attempts exceeded. Please request a new OTP.", exception.getMessage());
        verify(redisService).delete("forgot-password-otp:" + testEmail);
    }

    // ===== SET NEW PASSWORD AFTER VERIFICATION TESTS =====

    @Test
    void setNewPasswordAfterVerification_Success_UpdatesPassword() {
        // Given
        when(verificationTokenService.validateAndExtractEmail(testVerificationToken)).thenReturn(testEmail);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(normalUser);
        doNothing().when(verificationTokenService).invalidateToken(testVerificationToken);

        // When
        boolean result = authService.setNewPasswordAfterVerification(validSetPasswordRequest, testVerificationToken);

        // Then
        assertTrue(result);
        verify(verificationTokenService).validateAndExtractEmail(testVerificationToken);
        verify(userRepository).findByUserEmail(testEmail);
        verify(passwordEncoder).encode("NewPassword123");
        verify(userRepository).save(any(User.class));
        verify(verificationTokenService).invalidateToken(testVerificationToken);
    }

    @Test
    void setNewPasswordAfterVerification_ThrowsException_WhenEmailIsNull() {
        // Given
        SetNewPasswordRequestDto invalidRequest = SetNewPasswordRequestDto.builder()
                .email(null)
                .newPassword("NewPassword123")
                .verificationToken(testVerificationToken)
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.setNewPasswordAfterVerification(invalidRequest, testVerificationToken)
        );
        assertEquals("Email, new password, and verification token are required", exception.getMessage());
    }

    @Test
    void setNewPasswordAfterVerification_ThrowsException_WhenPasswordIsNull() {
        // Given
        SetNewPasswordRequestDto invalidRequest = SetNewPasswordRequestDto.builder()
                .email(testEmail)
                .newPassword(null)
                .verificationToken(testVerificationToken)
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.setNewPasswordAfterVerification(invalidRequest, testVerificationToken)
        );
        assertEquals("Email, new password, and verification token are required", exception.getMessage());
    }

    @Test
    void setNewPasswordAfterVerification_ThrowsException_WhenTokenIsNull() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.setNewPasswordAfterVerification(validSetPasswordRequest, null)
        );
        assertEquals("Email, new password, and verification token are required", exception.getMessage());
    }

    @Test
    void setNewPasswordAfterVerification_ThrowsException_WhenTokenIsInvalid() {
        // Given
        when(verificationTokenService.validateAndExtractEmail(testVerificationToken)).thenReturn(null);

        // When & Then
        InvalidOtpException exception = assertThrows(
                InvalidOtpException.class,
                () -> authService.setNewPasswordAfterVerification(validSetPasswordRequest, testVerificationToken)
        );
        assertEquals("Invalid or expired verification token", exception.getMessage());
    }

    @Test
    void setNewPasswordAfterVerification_ThrowsException_WhenEmailMismatch() {
        // Given
        when(verificationTokenService.validateAndExtractEmail(testVerificationToken)).thenReturn("different@example.com");

        // When & Then
        InvalidOtpException exception = assertThrows(
                InvalidOtpException.class,
                () -> authService.setNewPasswordAfterVerification(validSetPasswordRequest, testVerificationToken)
        );
        assertEquals("Email mismatch with verification token", exception.getMessage());
    }

    @Test
    void setNewPasswordAfterVerification_ThrowsException_WhenUserNotFound() {
        // Given
        when(verificationTokenService.validateAndExtractEmail(testVerificationToken)).thenReturn(testEmail);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.empty());

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.setNewPasswordAfterVerification(validSetPasswordRequest, testVerificationToken)
        );
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void setNewPasswordAfterVerification_ThrowsException_WhenUserIsGoogleAccount() {
        // Given
        when(verificationTokenService.validateAndExtractEmail(testVerificationToken)).thenReturn(testEmail);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(googleUser));

        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> authService.setNewPasswordAfterVerification(validSetPasswordRequest, testVerificationToken)
        );
        assertEquals("This account was created with Google login. Password reset is not available for Google accounts.", exception.getMessage());
    }

    // ===== CHANGE PASSWORD TESTS =====

    @Test
    void changePassword_Success_UpdatesPassword() {
        // Given
        doNothing().when(passwordValidationService).validateChangePasswordRequest(validChangePasswordRequest);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.of(normalUser));
        doNothing().when(passwordValidationService).validateUserCanChangePassword(normalUser);
        doNothing().when(passwordValidationService).validateOldPassword(normalUser, "OldPassword123");
        doNothing().when(passwordValidationService).validateNewPasswordDifferent(normalUser, "NewPassword123");
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(normalUser);

        // When
        boolean result = authService.changePassword(testEmail, validChangePasswordRequest);

        // Then
        assertTrue(result);
        verify(passwordValidationService).validateChangePasswordRequest(validChangePasswordRequest);
        verify(userRepository).findByUserEmail(testEmail);
        verify(passwordValidationService).validateUserCanChangePassword(normalUser);
        verify(passwordValidationService).validateOldPassword(normalUser, "OldPassword123");
        verify(passwordValidationService).validateNewPasswordDifferent(normalUser, "NewPassword123");
        verify(passwordEncoder).encode("NewPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_ThrowsException_WhenUserNotFound() {
        // Given
        doNothing().when(passwordValidationService).validateChangePasswordRequest(validChangePasswordRequest);
        when(userRepository.findByUserEmail(testEmail)).thenReturn(Optional.empty());

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> authService.changePassword(testEmail, validChangePasswordRequest)
        );
        assertEquals("User not found with email: " + testEmail, exception.getMessage());
    }

    @Test
    void changePassword_ThrowsException_WhenValidationFails() {
        // Given
        doThrow(new IllegalArgumentException("Password validation failed"))
                .when(passwordValidationService).validateChangePasswordRequest(validChangePasswordRequest);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(testEmail, validChangePasswordRequest)
        );
        assertEquals("Password validation failed", exception.getMessage());
        verifyNoInteractions(userRepository);
    }
} 