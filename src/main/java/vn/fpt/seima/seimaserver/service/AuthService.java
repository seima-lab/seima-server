package vn.fpt.seima.seimaserver.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.fpt.seima.seimaserver.dto.request.auth.NormalRegisterRequestDto;

public interface AuthService {
    void logout(HttpServletRequest request);

    String processRegister( NormalRegisterRequestDto normalRegisterRequestDto);
}
