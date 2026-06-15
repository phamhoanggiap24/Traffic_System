package com.traffic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private com.traffic.service.JwtService jwtService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                )

                // CẤU HÌNH BẮT LỖI CHUẨN: Chỉ trả về 401 khi thực sự không có Token hợp lệ
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\": 401, \"message\": \"Phiên đăng nhập không hợp lệ hoặc đã hết hạn!\"}");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // 🌟 ĐÃ CHỈNH: Cho phép tất cả tài khoản đã Login đi qua tầng Security để vào xử lý sâu trong Controller
                        .requestMatchers("/api/profile/me").authenticated()
                        .requestMatchers("/api/profile/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN", "USER", "ADMIN")

                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers("/api/traffic/**").authenticated()
                        .requestMatchers("/api/bao-cao/**").authenticated()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtService, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"status\": 200, \"message\": \"Đăng xuất thành công!\"}");
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(frontendUrl, "https://traffic-system-vn.vercel.app"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}