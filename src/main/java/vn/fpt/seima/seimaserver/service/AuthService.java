package vn.fpt.seima.seimaserver.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    void logout(HttpServletRequest request);
}
