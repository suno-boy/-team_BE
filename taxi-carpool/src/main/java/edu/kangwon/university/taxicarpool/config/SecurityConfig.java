package edu.kangwon.university.taxicarpool.config;

import static org.springframework.security.config.Customizer.withDefaults;

import edu.kangwon.university.taxicarpool.auth.JwtAuthenticationFilter;
import edu.kangwon.university.taxicarpool.auth.JwtUtil;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    public SecurityConfig(JwtUtil jwtUtil, MemberRepository memberRepository) { // ★ 변경
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, memberRepository);

        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",                        // 루트
                    "/chat/**",                 // websocket
                    "/api/auth/**",             // 회원가입, 로그인
                    "/api/email/**",            // 이메일 인증
                    "/api/password/reset-link", // 비밀번호 재설정 링크
                    "/api/password/reset",      // 비밀번호 재설정
                    "/swagger-ui.html",         // 스웨거 UI
                    "/swagger-ui/**",           // 스웨거 UI리소스
                    "/api-docs/**",          // 스웨거 API 문서
                    "/swagger-resources/**",    // 스웨거 리소스
                    "/webjars/**",              // 스웨거 관련 정적 리소스
                    "/h2-console/**"
                ).permitAll()

                .requestMatchers(HttpMethod.GET,
                    "/api/map/search"
                ).permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");           // 모든 출처 허용
        config.addAllowedMethod("*");                  // 모든 HTTP 메서드 허용
        config.addAllowedHeader("*");                  // 모든 헤더 허용
        config.setAllowCredentials(true);              // 쿠키/자격증명 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}