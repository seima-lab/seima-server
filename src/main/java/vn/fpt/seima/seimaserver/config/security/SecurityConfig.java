package vn.fpt.seima.seimaserver.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/",
                                        "/login",
                                        // "/register", // Cân nhắc bỏ nếu không có trang register tổng
                                        "/api/auth/otp/request",
                                        "/api/auth/otp/verify",
                                        "/api/auth/register" // Endpoint hoàn tất đăng ký
                                ).permitAll()
                                .anyRequest().authenticated()
                );
                // Cấu hình OAuth2 login này yêu cầu spring-boot-starter-oauth2-client
                /*.oauth2Login(oauth2Login ->
                        oauth2Login.defaultSuccessUrl("/", true)
                );*/
        return http.build();
    }
}