package edu.kangwon.university.taxicarpool.party.PartyUtil;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PartySearchFilter {

    private final Double depLng;
    private final Double depLat;
    private final Double dstLng;
    private final Double dstLat;
    private final LocalDateTime depTime;

    public boolean hasDeparture() {
        return depLng != null && depLat != null;
    }

    public boolean hasDestination() {
        return dstLng != null && dstLat != null;
    }

    public boolean hasTime() {
        return depTime != null;
    }
}