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
import vn.fpt.seima.seimaserver.exception.GoogleAccountConflictException;

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
        boolean isUserActive;

        // Tìm user theo email (không phân biệt trạng thái active để tránh tạo duplicate)
        Optional<User> existingUserOpt = userRepository.findByUserEmail(email);
        User userEntity;

        if (existingUserOpt.isEmpty()) {
            // User chưa từng login, tạo mới
            isFirstLoginToApp = true;
            isUserActive = false;
            userEntity = User.builder()
                    .userEmail(email)
                    .userFullName(fullName)
                    .userAvatarUrl(avatarUrl)
                    .userIsActive(isUserActive)
                    .isLogByGoogle(true)
                    .userGender(true)
                    .build();
            userEntity = userRepository.save(userEntity);
        } else {
            userEntity = existingUserOpt.get();
            
            // Check if user registered normally (not with Google) - prevent Google login
            if (!userEntity.getIsLogByGoogle()) {
                throw new GoogleAccountConflictException("An account with this email already exists. Please use your password to login instead of Google login.");
            }
            
            // Kiểm tra trạng thái user để xác định logic xử lý
            if (userEntity.getUserIsActive()) {
                // User đã active - đây là login bình thường
                isFirstLoginToApp = false;
                isUserActive = true;
            } else {
                // User đã tồn tại nhưng chưa active (chưa hoàn thành setup)
                // Đây vẫn được coi là first login để user có thể tiếp tục setup
                isFirstLoginToApp = true;
                isUserActive = false;
            }
            
            // Cập nhật thông tin từ Google nếu có thay đổi
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
                .isUserActive(isUserActive)
                .build();
    }
}
