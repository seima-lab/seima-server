package vn.fpt.seima.seimaserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ForgotPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.LoginRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ResetPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.LoginResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.NormalRegisterResponseDto;

public interface AuthService {
    void logout(HttpServletRequest request);

    NormalRegisterResponseDto processRegister(NormalRegisterRequestDto normalRegisterRequestDto);
    
    boolean verifyOtp(VerifyOtpRequestDto verifyOtpRequestDto);
    
    void resendOtp(String email);
    
    LoginResponseDto login(LoginRequestDto loginRequestDto);
    
    void forgotPassword(ForgotPasswordRequestDto forgotPasswordRequestDto);
    
    boolean resetPassword(ResetPasswordRequestDto resetPasswordRequestDto);
    
    void resendForgotPasswordOtp(String email);
    
    boolean changePassword(String userEmail, ChangePasswordRequestDto changePasswordRequestDto);
}
