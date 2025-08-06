package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;
import vn.fpt.seima.seimaserver.entity.User;

/**
 * Service for password-related validation operations
 * Separated for better testability and single responsibility
 */
public interface PasswordValidationService {
    
    /**
     * Validates the change password request
     */
    void validateChangePasswordRequest(ChangePasswordRequestDto request);
    
    /**
     * Validates that the old password matches the user's current password
     */
    void validateOldPassword(User user, String oldPassword);
    
    /**
     * Validates that the user can change password (not a Google account)
     */
    void validateUserCanChangePassword(User user);
    
    /**
     * Validates that the new password is different from the old password
     */
    void validateNewPasswordDifferent(User user, String newPassword);
} 