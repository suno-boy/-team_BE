package edu.kangwon.university.taxicarpool.chatting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException;
import edu.kangwon.university.taxicarpool.profanity.ProfanityService;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class JwtStompInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtStompInterceptor.class);

    private final ProfanityService profanityService;
    private final ObjectMapper objectMapper;

    public JwtStompInterceptor(ProfanityService profanityService,
        com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.profanityService = profanityService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        log.debug("STOMP preSend ▸ sessionId={}, command={}, user={}",
            accessor.getSessionId(), command, accessor.getUser());

        // CONNECT
        if (StompCommand.CONNECT.equals(command)) {
            if (accessor.getUser() == null) {
                log.error("STOMP CONNECT 실패 – user가 없습니다.");
                throw new TokenInvalidException("WebSocket 연결 실패: 인증 정보가 없습니다.");
            }
        }
        // SUBSCRIBE & SEND
        else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            if (accessor.getUser() == null) {
                log.error("STOMP {} 실패 – user가 없습니다.", command);
                throw new TokenInvalidException("WebSocket 연결 실패: 인증 정보가 없습니다.");
            }
        }

        // ▼ SEND 메시지의 본문(content) 마스킹 (전송 UX 보호)
        if (StompCommand.SEND.equals(command)) {
            try {
                Object payload = message.getPayload();
                if (payload instanceof byte[] bytes) {
                    // 메시지(JSON) 파싱 → content 필드만 마스킹
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    var tree = objectMapper.readTree(json);
                    if (tree.has("content")) {
                        String original = tree.get("content").asText();
                        String masked = profanityService.maskSmart(original); // 비속어 마스킹 처리
                        ((ObjectNode) tree)
                            .put("content", masked);

                        byte[] replaced = objectMapper.writeValueAsBytes(tree);
                        return MessageBuilder
                            .withPayload(replaced)
                            .copyHeaders(message.getHeaders())
                            .build();
                    }
                }
            } catch (Exception e) {
                // 파싱 실패 시에는 그냥 원본 통과(UX를 위해 채팅 자체를 막지는 않음)
                log.warn("STOMP SEND payload masking skipped: {}", e.toString());
            }
        }

        return message;
    }

}