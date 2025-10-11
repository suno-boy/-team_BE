package edu.kangwon.university.taxicarpool.party.dto;

import edu.kangwon.university.taxicarpool.map.MapPlaceDTO;
import edu.kangwon.university.taxicarpool.profanity.NoProfanity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PartyUpdateRequestDTO {

    private PartyOptionDTO options;

    @NotNull(message = "출발 시간 입력은 필수입니다.")
    private LocalDateTime startDateTime;

    @Size(max = 30, message = "설명은 최대 30글자입니다.")
    @NoProfanity(message = "설명에 비속어가 포함되어 있습니다.")
    private String comment;

    @Max(value = 4, message = "택시의 최대 탑승 인원 수는 4명입니다.")
    private int maxParticipantCount;

    private MapPlaceDTO startPlace;
    private MapPlaceDTO endPlace;

    @NoProfanity(message = "공지에 비속어가 포함되어 있습니다.")
    private String notification;
}
