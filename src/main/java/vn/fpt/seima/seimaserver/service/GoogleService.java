package vn.fpt.seima.seimaserver.service;

import jakarta.validation.Valid;
import vn.fpt.seima.seimaserver.dto.response.auth.GoogleLoginResponseDto;

public interface GoogleService {
    GoogleLoginResponseDto processGoogleLogin(@Valid String idToken);
}
