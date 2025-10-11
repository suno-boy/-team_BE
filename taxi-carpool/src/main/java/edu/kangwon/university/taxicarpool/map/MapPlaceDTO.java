package edu.kangwon.university.taxicarpool.map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MapPlaceDTO {
    private final String name;
    private final String roadAddressName;
    private final Double x;
    private final Double y;
}
