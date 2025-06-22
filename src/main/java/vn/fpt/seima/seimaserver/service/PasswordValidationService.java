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
     * @param request the change password request
     * @throws vn.fpt.seima.seimaserver.exception.PasswordMismatchException if passwords don't match
     * @throws vn.fpt.seima.seimaserver.exception.NullRequestParamException if any required field is null/empty
     */
    void validateChangePasswordRequest(ChangePasswordRequestDto request);
    
    /**
     * Validates that the old password matches the user's current password
     * @param user the user entity
     * @param oldPassword the old password to verify
     * @throws vn.fpt.seima.seimaserver.exception.InvalidPasswordException if old password doesn't match
     */
    void validateOldPassword(User user, String oldPassword);
    
    /**
     * Validates that the user can change password (not a Google account)
     * @param user the user entity
     * @throws vn.fpt.seima.seimaserver.exception.GoogleAccountConflictException if user is Google account
     */
    void validateUserCanChangePassword(User user);
    
    /**
     * Validates that the new password is different from the old password
     * @param user the user entity
     * @param newPassword the new password
     * @throws IllegalArgumentException if passwords are the same
     */
    void validateNewPasswordDifferent(User user, String newPassword);
} 