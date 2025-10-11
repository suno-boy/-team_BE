package edu.kangwon.university.taxicarpool.chatting;

import edu.kangwon.university.taxicarpool.chatting.dto.MessageCreateDTO;
import edu.kangwon.university.taxicarpool.chatting.dto.MessageResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Tag(name = "ChatWebSocket", description = "WebSocket STOMP 채팅 메시지 발행 API")
@SecurityRequirement(name = "bearerAuth")
@Controller
public class ChatWebSocketController {

    private final ChattingService chattingService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChattingService chattingService,
        SimpMessagingTemplate messagingTemplate) {
        this.chattingService = chattingService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 클라이언트→서버 메시지 발행을 처리하고, 브로커로 브로드캐스트까지 수행. destination: /pub/party/{partyId}/message
     */
    @Operation(
        summary = "채팅 메시지 전송",
        description = "클라이언트→서버 STOMP 메시지 발행 후 브로커로 브로드캐스트합니다."
    )
    @MessageMapping("/party/{partyId}/message")
    public void sendMessage(@Parameter(
        description = "메시지를 발행할 파티 ID", required = true)
    @DestinationVariable Long partyId,
        @Payload MessageCreateDTO dto,
        Principal principal
    ) {
        Long memberId = Long.valueOf(principal.getName());
        // 서비스에서 메시지 저장 + DTO 변환
        MessageResponseDTO response =
            chattingService.sendMessage(partyId, memberId, dto.getContent());

        // 브로커로 브로드캐스트 (SUBSCRIBE prefix: /sub)
        messagingTemplate.convertAndSend(
            "/sub/party/" + partyId,
            response
        );
    }
}
