package vn.fpt.seima.seimaserver.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.response.user.UserInGoogleReponseDto;
import vn.fpt.seima.seimaserver.dto.response.auth.GoogleLoginResponseDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.GoogleIdTokenVerifierService;
import vn.fpt.seima.seimaserver.service.GoogleService;
import vn.fpt.seima.seimaserver.service.JwtService;

import java.util.Optional;

@Service
@AllArgsConstructor
public class GoogleServiceImpl implements GoogleService {

    private GoogleIdTokenVerifierService googleIdTokenVerifier;

    private JwtService jwtService;

    private UserRepository userRepository;



    @Override
    public GoogleLoginResponseDto processGoogleLogin(String idTokenString) {
        GoogleIdToken.Payload payload = googleIdTokenVerifier.verify(idTokenString);
        if (payload == null) {
            throw new IllegalArgumentException("Invalid Google ID Token");
        }

        String email = payload.getEmail();
        String fullName = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");
        boolean isFirstLoginToApp;

        Optional<User> existingUserOpt = userRepository.findByUserEmail(email);
        User userEntity;

        if (existingUserOpt.isEmpty()) {
            isFirstLoginToApp = true;
            userEntity = User.builder()
                    .userEmail(email)
                    .userFullName(fullName)
                    .userAvatarUrl(avatarUrl)
                    .userIsActive(false)
                    .isLogByGoogle(true)
                    .userGender(true)
                    .build();
            userEntity = userRepository.save(userEntity); // Lưu người dùng mới
        } else {
            isFirstLoginToApp = false;
            userEntity = existingUserOpt.get();
            // Người dùng đã tồn tại, có thể cập nhật thông tin nếu cần
            boolean needsUpdate = false;
            if (fullName != null && !fullName.equals(userEntity.getUserFullName())) {
                userEntity.setUserFullName(fullName);
                needsUpdate = true;
            }
            if (avatarUrl != null && !avatarUrl.equals(userEntity.getUserAvatarUrl())) {
                userEntity.setUserAvatarUrl(avatarUrl);
                needsUpdate = true;
            }
            if (needsUpdate) {
                userEntity = userRepository.save(userEntity);
            }
        }

        // Tạo DTO từ userEntity đã được đảm bảo tồn tại trong DB
        UserInGoogleReponseDto userDtoForResponseAndJwt = UserInGoogleReponseDto.builder()
                .email(userEntity.getUserEmail())
                .fullName(userEntity.getUserFullName())
                .avatarUrl(userEntity.getUserAvatarUrl())
                .build();

        String accessToken = jwtService.generateAccessToken(userDtoForResponseAndJwt);
        String refreshToken = jwtService.generateRefreshToken(userDtoForResponseAndJwt);

        return GoogleLoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInfomation(userDtoForResponseAndJwt)
                .isFirstLogin(isFirstLoginToApp)
                .build();
    }
}
