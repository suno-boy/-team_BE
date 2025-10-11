package edu.kangwon.university.taxicarpool.party.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartyOptionDTO {
    private boolean sameGenderOnly;
    private boolean costShareBeforeDropOff;
    private boolean quietMode;
    private boolean destinationChangeIn5Minutes;
}
