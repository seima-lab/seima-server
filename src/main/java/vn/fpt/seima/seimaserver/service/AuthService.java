package vn.fpt.seima.seimaserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;
import vn.fpt.seima.seimaserver.dto.request.auth.VerifyOtpRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.NormalRegisterResponseDto;

public interface AuthService {
    void logout(HttpServletRequest request);

    NormalRegisterResponseDto processRegister(NormalRegisterRequestDto normalRegisterRequestDto);
    
    boolean verifyOtp(VerifyOtpRequestDto verifyOtpRequestDto);
    
    void resendOtp(String email);
}
