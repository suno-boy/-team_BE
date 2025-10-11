package edu.kangwon.university.taxicarpool.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class MapSearchResponseDTO {
    private final List<MapPlaceDTO> places;
}
