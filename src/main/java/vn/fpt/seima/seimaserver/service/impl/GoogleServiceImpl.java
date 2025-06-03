package vn.fpt.seima.seimaserver.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.user.UserInGoogleReponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.GoogleLoginResponseDto;
import vn.fpt.seima.seimaserver.exception.GmailAlreadyExistException;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.GoogleIdTokenVerifierService;
import vn.fpt.seima.seimaserver.service.GoogleService;
import vn.fpt.seima.seimaserver.service.JwtService;

@Service
@AllArgsConstructor
public class GoogleServiceImpl implements GoogleService {

    private GoogleIdTokenVerifierService googleIdTokenVerifier;

    private JwtService jwtService;

    private UserRepository userRepository;



    @Override
    public GoogleLoginResponseDto processGoogleLogin(String idToken) {
        Boolean isFirstLogin = true;

        GoogleIdToken.Payload payload = googleIdTokenVerifier.verify(idToken);
        if(payload == null) {
            throw new IllegalArgumentException("Invalid Google ID Token");
        }
        String email = payload.getEmail();
        // 1.Check email is exists in database
        if(userRepository.findByUserEmail(email).isPresent()) {
           isFirstLogin = false;
        }

        String fullName = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");
        UserInGoogleReponseDto newUserInGoogleReponseDto = new UserInGoogleReponseDto(email, fullName, avatarUrl);
        String accessToken = jwtService.generateAccessToken(newUserInGoogleReponseDto);
        String refreshToken = jwtService.generateRefreshToken(newUserInGoogleReponseDto);
        return GoogleLoginResponseDto
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInfomation(newUserInGoogleReponseDto)
                .isFirstLogin(isFirstLogin)
                .build();
    }
}
