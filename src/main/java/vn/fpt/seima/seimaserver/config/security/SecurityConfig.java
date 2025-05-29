package vn.fpt.seima.seimaserver.config.security;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configurable
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/",
                                        "/login",
                                        "/register",
                                        "/api/auth/otp/request",
                                        "/api/auth/otp/verify"
                                ).permitAll() // Cho phép truy cập không cần xác thực
                                .anyRequest().authenticated() // Các yêu cầu khác cần xác thực
                ).oauth2Login(oauth2Login -> oauth2Login.defaultSuccessUrl("/", true));
        return http.build();
    }
}
