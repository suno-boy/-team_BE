package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.party.PartyDTO.PartyCreateRequestDTO;
import edu.kangwon.university.taxicarpool.party.PartyDTO.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.PartyDTO.PartyUpdateRequestDTO;
import org.springframework.stereotype.Component;


@Component
public class PartyMapper {

    public PartyResponseDTO convertToResponseDTO(PartyEntity partyEntity) {
        return new PartyResponseDTO(
            partyEntity.getId(),
            partyEntity.getName(),
            partyEntity.isDeleted(),
            partyEntity.getMemberEntities(),
            partyEntity.getHostMemberId(),
            partyEntity.getEndDate(),
            partyEntity.isSameGenderOnly(),
            partyEntity.isCostShareBeforeDropOff(),
            partyEntity.isQuietMode(),
            partyEntity.isDestinationChangeIn5Minutes()
        );
    }

    public PartyEntity convertToEntity(PartyCreateRequestDTO createRequestDTO) {
        return new PartyEntity(
            createRequestDTO.getName(),
            createRequestDTO.getMemberEntities(),
            createRequestDTO.getHostMemberId(),
            createRequestDTO.getEndDate(),
            createRequestDTO.isSameGenderOnly(),
            createRequestDTO.isCostShareBeforeDropOff(),
            createRequestDTO.isQuietMode(),
            createRequestDTO.isDestinationChangeIn5Minutes()
        );
    }

    public PartyEntity convertToEntityByUpdate(PartyEntity partyEntity, PartyUpdateRequestDTO partyUpdateRequestDTO) {
        return partyEntity.updateParty(
            partyUpdateRequestDTO.getName(),
            partyUpdateRequestDTO.isDeleted(),
            partyUpdateRequestDTO.getMemberEntities(),
            partyUpdateRequestDTO.getHostMemberId(),
            partyUpdateRequestDTO.getEndDate(),
            partyUpdateRequestDTO.isSameGenderOnly(),
            partyUpdateRequestDTO.isCostShareBeforeDropOff(),
            partyUpdateRequestDTO.isQuietMode(),
            partyUpdateRequestDTO.isDestinationChangeIn5Minutes()
        );
    }
}
