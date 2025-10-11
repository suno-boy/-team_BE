package edu.kangwon.university.taxicarpool.chatting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ParticipantResponseDTO {

    private final Long memberId;
    private final String nickname;
}
