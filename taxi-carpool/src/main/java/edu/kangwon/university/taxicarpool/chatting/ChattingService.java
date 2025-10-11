package edu.kangwon.university.taxicarpool.chatting;

import edu.kangwon.university.taxicarpool.chatting.dto.MessageResponseDTO;
import edu.kangwon.university.taxicarpool.chatting.dto.NotificationResponseDTO;
import edu.kangwon.university.taxicarpool.chatting.dto.ParticipantResponseDTO;
import edu.kangwon.university.taxicarpool.fcm.FcmPushService;
import edu.kangwon.university.taxicarpool.fcm.dto.PushMessageDTO;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import edu.kangwon.university.taxicarpool.party.PartyEntity;
import edu.kangwon.university.taxicarpool.party.PartyRepository;
import edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException;
import edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException;
import edu.kangwon.university.taxicarpool.profanity.ProfanityService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChattingService {

    private final MessageRepository messageRepository;
    private final PartyRepository partyRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberRepository memberRepository;
    private final ProfanityService profanityService;
    private final FcmPushService fcmPushService;

    ChattingService(MessageRepository messageRepository,
        PartyRepository partyRepository, MessageMapper messageMapper,
        SimpMessagingTemplate messagingTemplate, MemberRepository memberRepository,
        ProfanityService profanityService, FcmPushService fcmPushService) {
        this.messageRepository = messageRepository;
        this.partyRepository = partyRepository;
        this.messageMapper = messageMapper;
        this.messagingTemplate = messagingTemplate;
        this.memberRepository = memberRepository;
        this.profanityService = profanityService;
        this.fcmPushService = fcmPushService;
    }

    /**
     * 파티 입장/퇴장 시 시스템 메시지를 생성하고 구독자에게 브로드캐스트합니다.
     *
     * <p>TALK 타입은 시스템 메시지로 허용되지 않습니다.</p>
     *
     * @param partyEntity 대상 파티 엔티티
     * @param memberEntity 입·퇴장한 멤버 엔티티
     * @param type 시스템 메시지 타입(ENTER/LEAVE)
     * @throws java.lang.IllegalArgumentException type이 TALK인 경우
     */
    @Transactional
    public void createSystemMessage(PartyEntity partyEntity, MemberEntity memberEntity,
        MessageType type) {
        // PartyService에서 Party, Member 존재여부 및 참여여부 검증이 이루어지므로 여기서는 검증하지 않음
        if (type == MessageType.TALK) {
            throw new IllegalArgumentException("시스템 메시지는 TALK 타입을 가질 수 없습니다.");
        }

        String displayType = type.getDisplayName();
        String content = memberEntity.getNickname() + "님이 " + displayType + "하셨습니다.";

        MessageEntity message = new MessageEntity(partyEntity, memberEntity, content,
            type);
        messageRepository.save(message);

        // WebSocket을 통해 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/sub/party/" + partyEntity.getId(),
            messageMapper.convertToResponseDTO(message));
    }

    /**
     * 특정 파티의 메시지 히스토리를 조회합니다.
     *
     * <p>{@code afterMessageId}가 null이면 처음부터 {@code limit}개를, 값이 있으면 해당 ID보다 큰 메시지를
     * 오름차순으로 최대 {@code limit}개 조회합니다.</p>
     *
     * @param partyId 파티 ID
     * @param memberId 조회 요청 멤버 ID(파티 참여자여야 함)
     * @param afterMessageId 기준 메시지 ID(null 가능)
     * @param limit 최대 조회 개수
     * @return 메시지 응답 DTO 목록(오름차순 정렬)
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException
     *         요청 멤버가 파티에 속해있지 않은 경우
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessageHistory(Long partyId, Long memberId,
        Long afterMessageId, int limit) {

        validateMemberInParty(partyId, memberId);

        Pageable pageable = PageRequest.of(0, limit);
        List<MessageEntity> messages;

        if (afterMessageId == null) {
            // afterMessageId가 null인 경우 모든 메시지를 가져옴
            messages = messageRepository.findByPartyIdOrderByIdAsc(partyId, pageable);
        } else {
            // afterMessageId가 주어진 경우 해당 ID보다 큰 메시지들을 가져옴
            messages = messageRepository.findByPartyIdAndIdGreaterThanOrderByIdAsc(partyId,
                afterMessageId, pageable);
        }

        return messages.stream()
            .map(messageMapper::convertToResponseDTO).toList();

    }

    /**
     * 사용자가 해당 파티에 속해 있는지 검증합니다.
     *
     * @param partyId 파티 ID
     * @param memberId 멤버 ID
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException
     *         멤버가 파티에 속해있지 않은 경우
     */
    private void validateMemberInParty(Long partyId, Long memberId) {
        PartyEntity party = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException("파티를 찾을 수 없습니다."));

        boolean isMember = party.getMemberEntities().stream()
            .anyMatch(member -> member.getId().equals(memberId));

        if (!isMember) {
            throw new MemberNotInPartyException("해당 파티의 멤버가 아닙니다.");
        }
    }

    /**
     * 채팅 메시지를 전송(저장)하고, 파티원들에게 FCM 푸시 알림을 발송합니다.
     *
     * <p>비속어는 {@link edu.kangwon.university.taxicarpool.profanity.ProfanityService#maskSmart(String)}
     * 로 마스킹하여 저장합니다.</p>
     *
     * @param partyId 파티 ID
     * @param memberId 발신자 멤버 ID(파티 참여자여야 함)
     * @param content 원본 메시지 내용
     * @return 저장된 메시지의 응답 DTO
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         발신자 멤버를 찾을 수 없는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException
     *         발신자가 파티에 속해있지 않은 경우
     */
    @Transactional
    public MessageResponseDTO sendMessage(Long partyId, Long memberId, String content) {
        validateMemberInParty(partyId, memberId);

        PartyEntity party = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException("파티를 찾을 수 없습니다."));
        MemberEntity sender = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("멤버를 찾을 수 없습니다."));

        boolean isMember = party.getMemberEntities().stream()
            .anyMatch(m -> m.getId().equals(memberId));
        if (!isMember) {
            throw new MemberNotInPartyException("멤버가 해당 파티의 구성원이 아닙니다.");
        }

        String masked = profanityService.maskSmart(content);

        MessageEntity message = new MessageEntity(party, sender, masked, MessageType.TALK);
        messageRepository.save(message);

        // FCM 푸시 알림 발송
        // 1. 알림을 받을 파티원 목록 생성 (메시지 보낸 사람 제외)
        List<Long> recipientIds = party.getMemberEntities().stream()
            .map(MemberEntity::getId)
            .filter(id -> !id.equals(memberId)) // 발신자 제외
            .collect(Collectors.toList());

        // 2. 알림 받을 사람이 있으면 푸시 발송
        if (!recipientIds.isEmpty()) {
            PushMessageDTO pushMessage = PushMessageDTO.builder()
                .title(party.getName()) // 파티방 이름을 알림 제목으로
                .body(String.format("%s: %s", sender.getNickname(), masked)) // "닉네임: 메시지 내용"
                .type("CHAT_MESSAGE") // 클라이언트와 협의된 타입
                .build();

            // data 필드에 partyId, messageId 등을 추가하여 딥링크 및 추가 데이터 처리에 활용
            pushMessage.getData().put("partyId", String.valueOf(partyId));
            pushMessage.getData().put("senderNickname", sender.getNickname());

            fcmPushService.sendPushToUsers(recipientIds, pushMessage);
        }

        return messageMapper.convertToResponseDTO(message);
    }

    /**
     * 파티의 참가자 목록을 조회합니다.
     *
     * @param partyId 파티 ID
     * @param memberId 요청자 멤버 ID(파티 참여자여야 함)
     * @return 참가자 응답 DTO 목록(멤버 ID, 닉네임)
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException
     *         요청자가 파티에 속해있지 않은 경우
     */
    @Transactional(readOnly = true)
    public List<ParticipantResponseDTO> getParticipants(Long partyId, Long memberId) {
        PartyEntity party = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException("파티를 찾을 수 없습니다."));

        boolean isMember = party.getMemberEntities().stream()
            .anyMatch(m -> m.getId().equals(memberId));
        if (!isMember) {
            throw new MemberNotInPartyException("멤버가 해당 파티의 구성원이 아닙니다.");
        }

        return party.getMemberEntities().stream()
            .map(m -> new ParticipantResponseDTO(m.getId(), m.getNickname()))
            .toList();
    }

    /**
     * 파티 공지사항을 수정합니다.
     *
     * <p>호스트만 수정할 수 있습니다.</p>
     *
     * @param partyId 파티 ID
     * @param memberId 요청자 멤버 ID(호스트여야 함)
     * @param notification 변경할 공지사항 내용
     * @return 수정된 공지사항 응답 DTO(파티 ID, 공지사항)
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException
     *         요청자가 파티에 속해있지 않은 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException
     *         요청자가 호스트가 아닌 경우
     */
    @Transactional
    public NotificationResponseDTO updateNotification(Long partyId, Long memberId,
        String notification) {

        PartyEntity party = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException("파티를 찾을 수 없습니다."));

        boolean isMember = party.getMemberEntities().stream()
            .anyMatch(m -> m.getId().equals(memberId));
        if (!isMember) {
            throw new MemberNotInPartyException("멤버가 해당 파티의 구성원이 아닙니다.");
        }

        if (!party.getHostMemberId().equals(memberId)) {
            throw new UnauthorizedHostAccessException("호스트만 공지사항을 수정할 수 있습니다.");
        }

        party.setNotification(notification);
        PartyEntity saved = partyRepository.save(party);

        return new NotificationResponseDTO(saved.getId(), saved.getNotification());
    }

}
