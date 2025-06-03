package vn.fpt.seima.seimaserver.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityService {

    /**
     * Lấy đối tượng Authentication hiện tại từ SecurityContext.
     * @return Optional chứa Authentication nếu có, ngược lại là Optional rỗng.
     */
    public Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Lấy đối tượng Principal (thông tin chính của người dùng) hiện tại.
     * Principal có thể là UserDetails, OAuth2User, hoặc một String (username).
     * @return Optional chứa Principal nếu có người dùng được xác thực, ngược lại là Optional rỗng.
     */
    public Optional<Object> getCurrentPrincipal() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> !("anonymousUser".equals(principal))); // Bỏ qua anonymousUser
    }

    /**
     * Lấy username (hoặc ID định danh) của người dùng hiện tại.
     * @return Optional chứa username nếu có, ngược lại là Optional rỗng.
     */
    public Optional<String> getCurrentUsername() {
        return getCurrentAuthentication()
                .map(Authentication::getName); // Authentication.getName() thường trả về username
    }

     /**
     * Lấy thông tin người dùng dưới dạng UserDetails nếu principal là UserDetails.
     * @return Optional chứa UserDetails nếu có và đúng kiểu, ngược lại là Optional rỗng.
     */
    public Optional<UserDetails> getCurrentUserDetails() {
        return getCurrentPrincipal()
                .filter(UserDetails.class::isInstance)
                .map(UserDetails.class::cast);
    }

    /**
     * Lấy thông tin người dùng dưới dạng OAuth2User nếu principal là OAuth2User.
     * @return Optional chứa OAuth2User nếu có và đúng kiểu, ngược lại là Optional rỗng.
     */
    public Optional<OAuth2User> getCurrentOAuth2User() {
        return getCurrentPrincipal()
                .filter(OAuth2User.class::isInstance)
                .map(OAuth2User.class::cast);
    }

    /**
     * Lấy email của người dùng hiện tại, ưu tiên từ OAuth2User trước, sau đó là UserDetails (nếu username là email).
     * @return Optional chứa email nếu tìm thấy, ngược lại là Optional rỗng.
     */
    public Optional<String> getCurrentUserEmail() {
        Optional<OAuth2User> oauth2UserOpt = getCurrentOAuth2User();
        if (oauth2UserOpt.isPresent() && oauth2UserOpt.get().getAttribute("email") != null) {
            return Optional.ofNullable(oauth2UserOpt.get().getAttribute("email").toString());
        }

        Optional<UserDetails> userDetailsOpt = getCurrentUserDetails();
        if (userDetailsOpt.isPresent()) {
            // Giả định username trong UserDetails là email
            return Optional.of(userDetailsOpt.get().getUsername());
        }

        // Nếu principal là String (do JwtAuthenticationFilter đặt làm email)
        // thì getCurrentUsername() sẽ trả về email đó.
        return getCurrentUsername(); // Bỏ comment hoặc thêm logic tương tự
    }

    //Get current user's avatar URL if available
    public Optional<String> getCurrentUserAvatarUrl() {
        return getCurrentOAuth2User()
                .map(oauth2User -> {
                    Object pictureObj = oauth2User.getAttribute("picture");
                    return pictureObj != null ? pictureObj.toString() : null;
                }); // Đảm bảo xử lý null nếu "picture" không có hoặc null
    }

    // Bạn có thể thêm các method khác để lấy các thuộc tính cụ thể
    // ví dụ: getCurrentUserId(), getCurrentUserRoles(), ...
    // Nếu bạn có một đối tượng User entity tùy chỉnh được lưu trong Principal, bạn có thể thêm method:
    /*
    public Optional<YourCustomUserEntity> getCurrentAppUser() {
        return getCurrentPrincipal()
                .filter(YourCustomUserEntity.class::isInstance)
                .map(YourCustomUserEntity.class::cast);
    }
    */
}
