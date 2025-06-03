package vn.fpt.seima.seimaserver.controller;

import com.google.api.client.auth.openidconnect.IdToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.auth.GoogleLoginRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.GoogleLoginResponseDto;
import vn.fpt.seima.seimaserver.dto.response.user.UserInGoogleReponseDto;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.service.GoogleService;
import vn.fpt.seima.seimaserver.service.JwtService;
import vn.fpt.seima.seimaserver.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private GoogleService googleService;
    private JwtService jwtService;
    private UserRepository userRepository;
    private UserDetailsService userDetailsService;
    // Google Login
    @PostMapping("/google")
    public ApiResponse<Object> googleLogin(
            @Valid
            @RequestBody GoogleLoginRequestDto googleLoginRequestDto
            ) {
        try {
            System.out.println("🟢 Controller: /api/v1/auth/google called");
            String IdToken= googleLoginRequestDto.getIdToken();
            if (IdToken == null) {
                return ApiResponse.builder()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .message("IdToken is null")
                        .build();
            }
            GoogleLoginResponseDto data = googleService.processGoogleLogin(IdToken);
            return ApiResponse.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Google login successful")
                    .data(data)
                    .build();
        } catch (IllegalArgumentException e) {
            return ApiResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh token is missing or malformed"));
        }

        String refreshToken = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(refreshToken)) { // Basic validation: not expired, signature ok
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
            }

            String email = jwtService.extractEmail(refreshToken);
            // It's crucial to load UserDetails to ensure the user still exists and is active
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            User appUser = userRepository.findByUserEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found during refresh token validation"));


            // Check if the refresh token belongs to this user (if you store refresh tokens per user or have other checks)
            // For this example, extracting email and re-creating access token is sufficient if refresh token itself is valid.
            UserInGoogleReponseDto userInGoogleReponseDto = UserInGoogleReponseDto.
                    builder()
                    .email(appUser.getUserEmail())
                    .fullName(appUser.getUserFullName())
                    .avatarUrl(appUser.getUserAvatarUrl())
                    .build();
            String newAccessToken = jwtService.generateAccessToken(userInGoogleReponseDto);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (Exception e) {
            // Log the exception
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token: " + e.getMessage()));
        }
    }

}
