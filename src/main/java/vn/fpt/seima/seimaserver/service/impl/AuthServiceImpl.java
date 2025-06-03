package vn.fpt.seima.seimaserver.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.service.AuthService;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    @Override
    public void logout(HttpServletRequest request) {

    }
}
