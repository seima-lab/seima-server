package vn.fpt.seima.seimaserver.controller;

import com.google.api.client.auth.openidconnect.IdToken;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.auth.GoogleLoginRequestDto;
import vn.fpt.seima.seimaserver.dto.response.auth.GoogleLoginResponseDto;
import vn.fpt.seima.seimaserver.service.GoogleService;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthController.class);

    private GoogleService googleService;

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


    //Logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        logger.info("Processing logout request (dev mode - Bearer token)");
        SecurityContextHolder.clearContext(); // Xóa context bảo mật phía server
        return ResponseEntity.ok("Logout successful. Client should discard the JWT.");
    }
}
