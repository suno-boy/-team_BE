package edu.kangwon.university.taxicarpool.party.dto;

import edu.kangwon.university.taxicarpool.map.MapPlaceDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartyResponseDTO {

    private Long id;
    private String name;
    private boolean isDeleted;
    private List<Long> memberIds;
    private Long hostMemberId;
    private LocalDateTime endDate;
    private PartyOptionDTO options;
    private LocalDateTime startDateTime;
    private String comment;
    private int currentParticipantCount;
    private int maxParticipantCount;
    private MapPlaceDTO startPlace;
    private MapPlaceDTO endPlace;
    private String notification;
    private boolean savingsCalculated;
}
