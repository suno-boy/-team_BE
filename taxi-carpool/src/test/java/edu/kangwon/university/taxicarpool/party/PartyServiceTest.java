package edu.kangwon.university.taxicarpool.party;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.kangwon.university.taxicarpool.chatting.ChattingService;
import edu.kangwon.university.taxicarpool.chatting.MessageType;
import edu.kangwon.university.taxicarpool.member.Gender;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.party.dto.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.partyException.MemberAlreadyInPartyException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyFullException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @InjectMocks
    private PartyService partyService;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PartyMapper partyMapper;

    @Mock
    private ChattingService chattingService;

    private MemberEntity hostMember;
    private MemberEntity joinerMember;
    private PartyEntity party;

    @BeforeEach
    void setUp() throws Exception {
        hostMember = new MemberEntity("host@kangwon.ac.kr", "password", "호스트", Gender.MALE);
        joinerMember = new MemberEntity("joiner@kangwon.ac.kr", "password", "참여자", Gender.FEMALE);

        setId(hostMember, 1L);
        setId(joinerMember, 2L);

        party = new PartyEntity(
            hostMember.getId(),
            null,
            LocalDateTime.now().plusHours(1),
            "코멘트",
            1,
            4,
            null,
            null
        );
        setId(party, 100L);

        party.getMemberEntities().add(hostMember);
    }

    @Test
    @DisplayName("파티 참여 성공 테스트")
    void joinParty_Success() {
        Long partyId = 100L;
        Long joinerId = 2L;

        when(partyRepository.findByIdAndIsDeletedFalse(partyId)).thenReturn(Optional.of(party));
        when(memberRepository.findById(joinerId)).thenReturn(Optional.of(joinerMember));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(party);
        when(partyMapper.convertToResponseDTO(any(PartyEntity.class))).thenReturn(new PartyResponseDTO());

        PartyResponseDTO responseDTO = partyService.joinParty(partyId, joinerId);

        assertNotNull(responseDTO);
        ArgumentCaptor<PartyEntity> partyCaptor = ArgumentCaptor.forClass(PartyEntity.class);
        verify(partyRepository, times(1)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertEquals(2, savedParty.getCurrentParticipantCount());
        assertTrue(savedParty.getMemberEntities().contains(joinerMember));
        verify(chattingService, times(1)).createSystemMessage(any(PartyEntity.class), eq(joinerMember), eq(MessageType.ENTER));
    }

    @Test
    @DisplayName("파티 퇴장 성공 테스트 (호스트가 아닌 멤버)")
    void leaveParty_Success() {
        Long partyId = 100L;
        Long leaverId = 2L;

        party.join(joinerMember);
        party.setCurrentParticipantCount(2);

        when(partyRepository.findByIdAndIsDeletedFalse(partyId)).thenReturn(Optional.of(party));
        when(memberRepository.findById(leaverId)).thenReturn(Optional.of(joinerMember));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(party);
        when(partyMapper.convertToResponseDTO(any(PartyEntity.class))).thenReturn(new PartyResponseDTO());

        PartyResponseDTO responseDTO = partyService.leaveParty(partyId, leaverId);

        assertNotNull(responseDTO);
        ArgumentCaptor<PartyEntity> partyCaptor = ArgumentCaptor.forClass(PartyEntity.class);
        verify(partyRepository, times(1)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();
        assertEquals(1, savedParty.getCurrentParticipantCount());
        assertFalse(savedParty.getMemberEntities().contains(joinerMember));
        verify(chattingService, times(1)).createSystemMessage(any(PartyEntity.class), eq(joinerMember), eq(MessageType.LEAVE));
    }

    @Test
    @DisplayName("파티 참여 실패 - 파티 정원이 가득 찬 경우")
    void joinParty_Fail_PartyIsFull() throws Exception {
        party.setCurrentParticipantCount(2);
        setMaxParticipantCountViaReflection(party, 2);
        party.getMemberEntities().add(joinerMember);

        MemberEntity charlie = new MemberEntity("charlie@email.com", "pw", "Charlie", Gender.MALE);
        setId(charlie, 3L);

        Long partyId = party.getId();
        Long newMemberId = charlie.getId();

        when(partyRepository.findByIdAndIsDeletedFalse(partyId)).thenReturn(Optional.of(party));
        when(memberRepository.findById(newMemberId)).thenReturn(Optional.of(charlie));

        assertThrows(PartyFullException.class, () -> {
            partyService.joinParty(partyId, newMemberId);
        });

        verify(partyRepository, times(0)).save(any(PartyEntity.class));
    }

    @Test
    @DisplayName("파티 참여 실패 - 이미 참여한 멤버인 경우")
    void joinParty_Fail_MemberAlreadyInParty() {
        party.getMemberEntities().add(joinerMember);
        party.setCurrentParticipantCount(2);

        Long partyId = party.getId();
        Long existingMemberId = joinerMember.getId();

        when(partyRepository.findByIdAndIsDeletedFalse(partyId)).thenReturn(Optional.of(party));
        when(memberRepository.findById(existingMemberId)).thenReturn(Optional.of(joinerMember));

        assertThrows(MemberAlreadyInPartyException.class, () -> {
            partyService.joinParty(partyId, existingMemberId);
        });

        verify(partyRepository, times(0)).save(any(PartyEntity.class));
    }

    @Test
    @DisplayName("파티 퇴장 성공 - 호스트가 떠나고 새 호스트가 지정되는 경우")
    void leaveParty_Success_HostLeavesAndNewHostIsAssigned() {
        party.getMemberEntities().add(joinerMember);
        party.setCurrentParticipantCount(2);
        assertEquals(hostMember.getId(), party.getHostMemberId(), "테스트 시작 전 호스트는 hostMember여야 합니다.");

        Long partyId = party.getId();
        Long hostId = hostMember.getId();

        when(partyRepository.findByIdAndIsDeletedFalse(partyId)).thenReturn(Optional.of(party));
        when(memberRepository.findById(hostId)).thenReturn(Optional.of(hostMember));
        when(partyRepository.save(any(PartyEntity.class))).thenReturn(party);
        when(partyMapper.convertToResponseDTO(any(PartyEntity.class))).thenReturn(new PartyResponseDTO());

        partyService.leaveParty(partyId, hostId);

        ArgumentCaptor<PartyEntity> partyCaptor = ArgumentCaptor.forClass(PartyEntity.class);
        verify(partyRepository, times(1)).save(partyCaptor.capture());
        PartyEntity savedParty = partyCaptor.getValue();

        assertEquals(joinerMember.getId(), savedParty.getHostMemberId(), "남아있는 첫 번째 멤버가 새 호스트가 되어야 합니다.");
        assertEquals(1, savedParty.getCurrentParticipantCount());
        assertFalse(savedParty.getMemberEntities().contains(hostMember));
    }

    private void setId(Object target, Long id) throws Exception {
        Field idField;
        try {
            idField = target.getClass().getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            idField = target.getClass().getSuperclass().getDeclaredField("id");
        }
        idField.setAccessible(true);
        idField.set(target, id);
    }

    private void setMaxParticipantCountViaReflection(PartyEntity party, int count) throws Exception {
        Field maxCountField = PartyEntity.class.getDeclaredField("maxParticipantCount");
        maxCountField.setAccessible(true);
        maxCountField.set(party, count);
    }
}