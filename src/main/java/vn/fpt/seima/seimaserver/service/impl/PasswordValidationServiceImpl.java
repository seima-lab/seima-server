package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GoogleAccountConflictException;
import vn.fpt.seima.seimaserver.exception.InvalidPasswordException;
import vn.fpt.seima.seimaserver.exception.NullRequestParamException;
import vn.fpt.seima.seimaserver.exception.PasswordMismatchException;
import vn.fpt.seima.seimaserver.service.PasswordValidationService;

@Service
@RequiredArgsConstructor
public class PasswordValidationServiceImpl implements PasswordValidationService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void validateChangePasswordRequest(ChangePasswordRequestDto request) {
        if (request == null) {
            throw new NullRequestParamException("Change password request cannot be null");
        }
        
        if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
            throw new NullRequestParamException("Current password is required");
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new NullRequestParamException("New password is required");
        }
        
        if (request.getConfirmNewPassword() == null || request.getConfirmNewPassword().trim().isEmpty()) {
            throw new NullRequestParamException("Confirm new password is required");
        }
        
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new PasswordMismatchException("New password and confirm password do not match");
        }
    }

    @Override
    public void validateOldPassword(User user, String oldPassword) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new NullRequestParamException("Current password is required");
        }
        
        if (user.getUserPassword() == null) {
            throw new GoogleAccountConflictException("This account does not have a password set. Please use Google login.");
        }
        
        if (!passwordEncoder.matches(oldPassword, user.getUserPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
    }

    @Override
    public void validateUserCanChangePassword(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (user.getIsLogByGoogle() != null && user.getIsLogByGoogle()) {
            throw new GoogleAccountConflictException("Google account users cannot change password. Please use Google account management.");
        }
        
        if (user.getUserPassword() == null) {
            throw new GoogleAccountConflictException("This account does not have a password set. Please use Google login.");
        }
        
        if (!user.getUserIsActive()) {
            throw new IllegalStateException("Account is not active");
        }
    }

    @Override
    public void validateNewPasswordDifferent(User user, String newPassword) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new NullRequestParamException("New password is required");
        }
        
        if (user.getUserPassword() != null && passwordEncoder.matches(newPassword, user.getUserPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
    }
} 