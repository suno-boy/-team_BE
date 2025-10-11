package edu.kangwon.university.taxicarpool.chatting.dto;

import edu.kangwon.university.taxicarpool.chatting.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class MessageResponseDTO {

    private final Long id;
    private final Long senderId;
    private final String senderNickname; // 프론트에서 이름을 알기 위해 추가적인 api 호출 방지
    private final String content;
    private final LocalDateTime createdAt;
    private final MessageType type;
}
