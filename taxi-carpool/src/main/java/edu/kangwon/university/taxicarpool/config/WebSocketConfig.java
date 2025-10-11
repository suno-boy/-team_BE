package edu.kangwon.university.taxicarpool.config;

import edu.kangwon.university.taxicarpool.chatting.JwtHandshakeInterceptor;
import edu.kangwon.university.taxicarpool.chatting.JwtStompInterceptor;
import java.security.Principal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker // STOMP 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompInterceptor jwtStompInterceptor;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Autowired
    public WebSocketConfig(JwtStompInterceptor jwtStompInterceptor,
        JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.jwtStompInterceptor = jwtStompInterceptor;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // STOMP 메시지 브로커 설정
        registry.enableSimpleBroker("/sub"); // 구독
        // 1. 메시지 브로커 활성화
        // 2. 구독 경로 설정(앱->서버)
        // 3. 서버->앱 메시지 전송 경로
        registry.setApplicationDestinationPrefixes("/pub"); // 발행
        // 1. 앱->서버 메시지 전송 경로
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 설정(이렇게만 해줘도 통신 터널을 열어두는 로직 반영됨)
        registry.addEndpoint("/chat")
            .addInterceptors(jwtHandshakeInterceptor)
            .setHandshakeHandler(new DefaultHandshakeHandler() {
                @Override
                protected Principal determineUser(ServerHttpRequest request,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {
                    // beforeHandshake 에서 저장된 Principal을 그대로 리턴(STOMP에서 쓰려고)
                    return (Principal) attributes.get("principal");
                }
            })
            .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // STOMP CONNECT 프레임을 가로채서 JWT 검증
        registration.interceptors(jwtStompInterceptor);
    }

}
