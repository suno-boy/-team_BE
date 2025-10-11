package edu.kangwon.university.taxicarpool.chatting;

import edu.kangwon.university.taxicarpool.auth.JwtUtil;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil, MemberRepository memberRepository) { // ★ 변경
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            writeUnauthorized(response, "WS-NO-TOKEN", "access_token이 없습니다.");
            return false;
        }

        jwtUtil.validateToken(token);

        Long userId = jwtUtil.getIdFromToken(token);
        int verInToken = jwtUtil.getTokenVersionFromToken(token);

        int verInDb = memberRepository.findTokenVersionById(userId);

        if (verInToken != verInDb) {
            writeUnauthorized(response, "WS-VERSION-MISMATCH",
                "다른 기기에서 더 최근에 로그인되어 현재 토큰이 무효화되었습니다. 다시 로그인해주세요.");
            return false;
        }

        // STOMP의 모든 단계에서 범용적으로 사용할 수 있도록 Principal로 저장
        UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(userId, null, null);
        attributes.put("principal", principal);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception) {
        // 형식적으로 훅 메서드만 생성
    }

    private String extractToken(ServerHttpRequest request) {
        MultiValueMap<String, String> params =
            UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        List<String> tokens = params.get("access_token");
        if (tokens != null && !tokens.isEmpty()) {
            return tokens.get(0);
        }
        return null;
    }

    private void writeUnauthorized(ServerHttpResponse response, String code, String message) {
        try {
            if (response instanceof ServletServerHttpResponse s) {
                HttpServletResponse r = s.getServletResponse();
                r.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                r.setCharacterEncoding("UTF-8");
                r.setContentType("application/json;charset=UTF-8");
                String json = "{\"status\":401,\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
                r.getWriter().write(json);
            } else {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
            }
        } catch (IOException ignore) { }
    }
}
