package edu.kangwon.university.taxicarpool.chatting;

import edu.kangwon.university.taxicarpool.chatting.dto.MessageResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    /**
     * MessageEntity를 MessageResponseDTO로 변환합니다.
     *
     * @param messageEntity 변환할 메시지 엔티티
     * @return 변환된 MessageResponseDTO 객체
     */
    public MessageResponseDTO convertToResponseDTO(MessageEntity messageEntity) {
        return new MessageResponseDTO(
            messageEntity.getId(),
            messageEntity.getSender() != null ? messageEntity.getSender().getId() : null,
            messageEntity.getSender() != null ? messageEntity.getSender().getNickname() : "탈퇴한 사용자",
            messageEntity.getContent(),
            messageEntity.getCreatedAt(),
            messageEntity.getType());
    }

}