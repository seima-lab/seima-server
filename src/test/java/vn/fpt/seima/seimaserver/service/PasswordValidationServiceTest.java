package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GoogleAccountConflictException;
import vn.fpt.seima.seimaserver.exception.InvalidPasswordException;
import vn.fpt.seima.seimaserver.exception.NullRequestParamException;
import vn.fpt.seima.seimaserver.exception.PasswordMismatchException;
import vn.fpt.seima.seimaserver.service.impl.PasswordValidationServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordValidationServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordValidationServiceImpl passwordValidationService;

    private ChangePasswordRequestDto validChangePasswordRequest;
    private User normalUser;
    private User googleUser;
    private User inactiveUser;
    private User userWithoutPassword;

    @BeforeEach
    void setUp() {
        // Valid change password request
        validChangePasswordRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword("NewPassword123")
                .confirmNewPassword("NewPassword123")
                .build();

        // Normal user with password
        normalUser = User.builder()
                .userEmail("test@example.com")
                .userPassword("encodedOldPassword")
                .isLogByGoogle(false)
                .userIsActive(true)
                .build();

        // Google user
        googleUser = User.builder()
                .userEmail("google@example.com")
                .userPassword(null)
                .isLogByGoogle(true)
                .userIsActive(true)
                .build();

        // Inactive user
        inactiveUser = User.builder()
                .userEmail("inactive@example.com")
                .userPassword("encodedPassword")
                .isLogByGoogle(false)
                .userIsActive(false)
                .build();

        // User without password
        userWithoutPassword = User.builder()
                .userEmail("nopassword@example.com")
                .userPassword(null)
                .isLogByGoogle(false)
                .userIsActive(true)
                .build();
    }

    // ===== VALIDATE CHANGE PASSWORD REQUEST TESTS =====

    @Test
    void validateChangePasswordRequest_Success_WithValidRequest() {
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> passwordValidationService.validateChangePasswordRequest(validChangePasswordRequest));
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenRequestIsNull() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(null)
        );
        assertEquals("Change password request cannot be null", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenOldPasswordIsNull() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword(null)
                .newPassword("NewPassword123")
                .confirmNewPassword("NewPassword123")
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("Current password is required", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenOldPasswordIsEmpty() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("")
                .newPassword("NewPassword123")
                .confirmNewPassword("NewPassword123")
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("Current password is required", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenOldPasswordIsWhitespace() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("   ")
                .newPassword("NewPassword123")
                .confirmNewPassword("NewPassword123")
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("Current password is required", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenNewPasswordIsNull() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword(null)
                .confirmNewPassword("NewPassword123")
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("New password is required", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenNewPasswordIsEmpty() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword("")
                .confirmNewPassword("NewPassword123")
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("New password is required", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenConfirmPasswordIsNull() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword("NewPassword123")
                .confirmNewPassword(null)
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("Confirm new password is required", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenConfirmPasswordIsEmpty() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword("NewPassword123")
                .confirmNewPassword("")
                .build();

        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("Confirm new password is required", exception.getMessage());
    }

    @Test
    void validateChangePasswordRequest_ThrowsException_WhenPasswordsDoNotMatch() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword("NewPassword123")
                .confirmNewPassword("DifferentPassword123")
                .build();

        // When & Then
        PasswordMismatchException exception = assertThrows(
                PasswordMismatchException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("New password and confirm password do not match", exception.getMessage());
    }

    // ===== VALIDATE OLD PASSWORD TESTS =====

    @Test
    void validateOldPassword_Success_WithValidPassword() {
        // Given
        when(passwordEncoder.matches("OldPassword123", "encodedOldPassword")).thenReturn(true);

        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> passwordValidationService.validateOldPassword(normalUser, "OldPassword123"));
        verify(passwordEncoder).matches("OldPassword123", "encodedOldPassword");
    }

    @Test
    void validateOldPassword_ThrowsException_WhenUserIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordValidationService.validateOldPassword(null, "OldPassword123")
        );
        assertEquals("User cannot be null", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateOldPassword_ThrowsException_WhenOldPasswordIsNull() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateOldPassword(normalUser, null)
        );
        assertEquals("Current password is required", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateOldPassword_ThrowsException_WhenOldPasswordIsEmpty() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateOldPassword(normalUser, "")
        );
        assertEquals("Current password is required", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateOldPassword_ThrowsException_WhenOldPasswordIsWhitespace() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateOldPassword(normalUser, "   ")
        );
        assertEquals("Current password is required", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateOldPassword_ThrowsException_WhenUserHasNoPassword() {
        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> passwordValidationService.validateOldPassword(userWithoutPassword, "OldPassword123")
        );
        assertEquals("This account does not have a password set. Please use Google login.", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateOldPassword_ThrowsException_WhenPasswordDoesNotMatch() {
        // Given
        when(passwordEncoder.matches("WrongPassword123", "encodedOldPassword")).thenReturn(false);

        // When & Then
        InvalidPasswordException exception = assertThrows(
                InvalidPasswordException.class,
                () -> passwordValidationService.validateOldPassword(normalUser, "WrongPassword123")
        );
        assertEquals("Current password is incorrect", exception.getMessage());
        verify(passwordEncoder).matches("WrongPassword123", "encodedOldPassword");
    }

    // ===== VALIDATE USER CAN CHANGE PASSWORD TESTS =====

    @Test
    void validateUserCanChangePassword_Success_WithNormalUser() {
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> passwordValidationService.validateUserCanChangePassword(normalUser));
    }

    @Test
    void validateUserCanChangePassword_ThrowsException_WhenUserIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordValidationService.validateUserCanChangePassword(null)
        );
        assertEquals("User cannot be null", exception.getMessage());
    }

    @Test
    void validateUserCanChangePassword_ThrowsException_WhenUserIsGoogleAccount() {
        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> passwordValidationService.validateUserCanChangePassword(googleUser)
        );
        assertEquals("Google account users cannot change password. Please use Google account management.", exception.getMessage());
    }

    @Test
    void validateUserCanChangePassword_ThrowsException_WhenUserHasNoPassword() {
        // When & Then
        GoogleAccountConflictException exception = assertThrows(
                GoogleAccountConflictException.class,
                () -> passwordValidationService.validateUserCanChangePassword(userWithoutPassword)
        );
        assertEquals("This account does not have a password set. Please use Google login.", exception.getMessage());
    }

    @Test
    void validateUserCanChangePassword_ThrowsException_WhenUserIsInactive() {
        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> passwordValidationService.validateUserCanChangePassword(inactiveUser)
        );
        assertEquals("Account is not active", exception.getMessage());
    }

    @Test
    void validateUserCanChangePassword_Success_WhenIsLogByGoogleIsNull() {
        // Given
        User userWithNullGoogleFlag = User.builder()
                .userEmail("test@example.com")
                .userPassword("encodedPassword")
                .isLogByGoogle(null)
                .userIsActive(true)
                .build();

        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> passwordValidationService.validateUserCanChangePassword(userWithNullGoogleFlag));
    }

    @Test
    void validateUserCanChangePassword_Success_WhenIsLogByGoogleIsFalse() {
        // Given
        User userWithFalseGoogleFlag = User.builder()
                .userEmail("test@example.com")
                .userPassword("encodedPassword")
                .isLogByGoogle(false)
                .userIsActive(true)
                .build();

        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> passwordValidationService.validateUserCanChangePassword(userWithFalseGoogleFlag));
    }

    // ===== VALIDATE NEW PASSWORD DIFFERENT TESTS =====

    @Test
    void validateNewPasswordDifferent_Success_WithDifferentPassword() {
        // Given
        when(passwordEncoder.matches("NewPassword123", "encodedOldPassword")).thenReturn(false);

        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> passwordValidationService.validateNewPasswordDifferent(normalUser, "NewPassword123"));
        verify(passwordEncoder).matches("NewPassword123", "encodedOldPassword");
    }

    @Test
    void validateNewPasswordDifferent_ThrowsException_WhenUserIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordValidationService.validateNewPasswordDifferent(null, "NewPassword123")
        );
        assertEquals("User cannot be null", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateNewPasswordDifferent_ThrowsException_WhenNewPasswordIsNull() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateNewPasswordDifferent(normalUser, null)
        );
        assertEquals("New password is required", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateNewPasswordDifferent_ThrowsException_WhenNewPasswordIsEmpty() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateNewPasswordDifferent(normalUser, "")
        );
        assertEquals("New password is required", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateNewPasswordDifferent_ThrowsException_WhenNewPasswordIsWhitespace() {
        // When & Then
        NullRequestParamException exception = assertThrows(
                NullRequestParamException.class,
                () -> passwordValidationService.validateNewPasswordDifferent(normalUser, "   ")
        );
        assertEquals("New password is required", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validateNewPasswordDifferent_ThrowsException_WhenPasswordsAreSame() {
        // Given
        when(passwordEncoder.matches("SamePassword123", "encodedOldPassword")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> passwordValidationService.validateNewPasswordDifferent(normalUser, "SamePassword123")
        );
        assertEquals("New password must be different from current password", exception.getMessage());
        verify(passwordEncoder).matches("SamePassword123", "encodedOldPassword");
    }

    @Test
    void validateNewPasswordDifferent_Success_WhenUserHasNoPassword() {
        // Given - user without password should be able to set any new password
        
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> passwordValidationService.validateNewPasswordDifferent(userWithoutPassword, "NewPassword123"));
        verifyNoInteractions(passwordEncoder);
    }

    // ===== INTEGRATION-STYLE TESTS =====

    @Test
    void completeChangePasswordValidation_Success_WithValidData() {
        // Given
        when(passwordEncoder.matches("OldPassword123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123", "encodedOldPassword")).thenReturn(false);

        // When & Then - simulate the complete validation flow
        assertDoesNotThrow(() -> {
            passwordValidationService.validateChangePasswordRequest(validChangePasswordRequest);
            passwordValidationService.validateUserCanChangePassword(normalUser);
            passwordValidationService.validateOldPassword(normalUser, "OldPassword123");
            passwordValidationService.validateNewPasswordDifferent(normalUser, "NewPassword123");
        });

        // Verify interactions
        verify(passwordEncoder).matches("OldPassword123", "encodedOldPassword");
        verify(passwordEncoder).matches("NewPassword123", "encodedOldPassword");
    }

    @Test
    void completeChangePasswordValidation_FailsAtFirstStep_WhenRequestInvalid() {
        // Given
        ChangePasswordRequestDto invalidRequest = ChangePasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .newPassword("NewPassword123")
                .confirmNewPassword("DifferentPassword")
                .build();

        // When & Then
        PasswordMismatchException exception = assertThrows(
                PasswordMismatchException.class,
                () -> passwordValidationService.validateChangePasswordRequest(invalidRequest)
        );
        assertEquals("New password and confirm password do not match", exception.getMessage());
        
        // Should not proceed to other validations
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void edgeCases_HandleNullAndEmptyValues() {
        // Test with user having null isLogByGoogle but valid password
        User userWithNullGoogleFlag = User.builder()
                .userEmail("test@example.com")
                .userPassword("encodedPassword")
                .isLogByGoogle(null)
                .userIsActive(true)
                .build();

        // Should not throw exception for null isLogByGoogle
        assertDoesNotThrow(() -> passwordValidationService.validateUserCanChangePassword(userWithNullGoogleFlag));
        
        // Test with empty string passwords
        assertThrows(NullRequestParamException.class,
                () -> passwordValidationService.validateOldPassword(normalUser, ""));
        
        assertThrows(NullRequestParamException.class,
                () -> passwordValidationService.validateNewPasswordDifferent(normalUser, ""));
    }
} 