package edu.kangwon.university.taxicarpool.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kangwon.university.taxicarpool.auth.authException.TokenExpiredException;
import edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, MemberRepository memberRepository) {
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain)
        throws ServletException, IOException {

        // 1) 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // "Bearer " 이후 토큰 부분임. 실제 엑세스 토큰.

            try {
                // 2) 토큰 검증
                if (jwtUtil.validateToken(token)) {
                    // 3) 토큰에서 사용자 식별값(id) 추출
                    Long id = jwtUtil.getIdFromToken(token);
                    int verInToken = jwtUtil.getTokenVersionFromToken(token);

                    // 3-1) DB의 최신 tokenVersion 조회
                    int verInDb = memberRepository.findTokenVersionById(id);

                    // 3-2) 버전 불일치면 "다른 기기에서 더 최근에 로그인" → 401
                    if (verInToken != verInDb) {
                        writeUnauthorized(response, "AUTH-VERSION-MISMATCH",
                            "다른 기기에서 더 최근에 로그인되어 현재 토큰이 무효화되었습니다. 다시 로그인해주세요.");
                        return;
                    }

                    // 4) 인증 객체 생성
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(id, null, null);
                    
                    // 5) SecurityContextHolder에 등록(한 번의 request에서 필요한 사용자 정보를 공유하여 꺼내쓰기)
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (TokenExpiredException e) {
                // 토큰 만료 시 401 응답 -> 프론트측에서 해당 예외처리 받고 Axios를 통해 재요청 보내기
                writeUnauthorized(response, "AUTH-EXPIRED", "Access 토큰이 만료되었습니다.");
                return;
            } catch (TokenInvalidException e) {
                writeUnauthorized(response, "AUTH-INVALID", "유효하지 않은 토큰입니다.");
                return;
            }
        }
        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = Map.of(
            "status", 401,
            "code", code,
            "message", message,        // 한글 메시지 OK
            "timestamp", System.currentTimeMillis()
        );
        new ObjectMapper().writeValue(response.getWriter(), body);
    }

}
