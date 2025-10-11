package edu.kangwon.university.taxicarpool.fcm.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PushMessageDTO {

    private String title;

    private String body;

    @Builder.Default
    private Map<String, String> data = new HashMap<>();

    private String type; // "CHAT", "PARTY_LEAVE", "DEPARTURE_REMINDER"
}
