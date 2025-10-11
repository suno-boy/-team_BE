package edu.kangwon.university.taxicarpool.chatting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {

    private final Long partyId;
    private final String notification;
}
