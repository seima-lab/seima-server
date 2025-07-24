package vn.fpt.seima.seimaserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.fpt.seima.seimaserver.dto.request.auth.ChangePasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ForgotPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.LoginRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.ResetPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.SetNewPasswordRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyForgotPasswordOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.LoginResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.NormalRegisterResponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.VerifyForgotPasswordOtpResponseDto;

public interface AuthService {


    NormalRegisterResponseDto processRegister(NormalRegisterRequestDto normalRegisterRequestDto);
    
    boolean verifyOtp(VerifyOtpRequestDto verifyOtpRequestDto);
    
    void resendOtp(String email);
    
    LoginResponseDto login(LoginRequestDto loginRequestDto);
    
    void forgotPassword(ForgotPasswordRequestDto forgotPasswordRequestDto);
    
    VerifyForgotPasswordOtpResponseDto verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequestDto verifyForgotPasswordOtpRequestDto);
    
    boolean setNewPasswordAfterVerification(SetNewPasswordRequestDto setNewPasswordRequestDto, String verificationToken);

    void resendForgotPasswordOtp(String email);
    
    boolean changePassword(String userEmail, ChangePasswordRequestDto changePasswordRequestDto);
}
