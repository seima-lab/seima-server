// package vn.fpt.seima.seimaserver.config.security; // Hoặc package security
package vn.fpt.seima.seimaserver.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.fpt.seima.seimaserver.service.JwtService;
import vn.fpt.seima.seimaserver.service.TokenBlacklistService;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService; // Inject Spring Security's UserDetailsService

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        System.out.println("🔍 JwtAuthFilter check path: " + path);


        // ✅ Bỏ qua những API không cần JWT
        if (path.equals("/api/v1/auth/google") ||
                path.equals("/api/v1/auth/refresh") ||
                path.equals("/api/v1/auth/logout") ||
                path.equals("/api/v1/auth/register")||
                path.equals("/api/v1/auth/verify-otp")||
                path.equals("/api/v1/auth/resend-otp")||
                path.equals("/api/v1/auth/login")||
                path.equals("/api/v1/auth/forgot-password")||
                path.equals("/api/v1/auth/hehe")||
                path.equals("/api/v1/auth/reset-password")
        ) {
            System.out.println("✅ Bypass JWT Filter for: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                // Token is blacklisted, don't process authentication
                filterChain.doFilter(request, response);
                return;
            }

            userEmail = jwtService.extractEmail(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    logger.warn("JWT token validation failed for user: " + userEmail);
                }
            }
        } catch (Exception e) {
            logger.warn("JWT token processing error: " + e.getMessage());
        }
        System.out.println("➡️ JWT filter passed, moving on");
        filterChain.doFilter(request, response);
    }

}