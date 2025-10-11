package edu.kangwon.university.taxicarpool.party;


import edu.kangwon.university.taxicarpool.chatting.ChattingService;
import edu.kangwon.university.taxicarpool.chatting.MessageType;
import edu.kangwon.university.taxicarpool.fcm.FcmPushService;
import edu.kangwon.university.taxicarpool.fcm.dto.PushMessageDTO;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import edu.kangwon.university.taxicarpool.party.PartyUtil.PartySearchFilter;
import edu.kangwon.university.taxicarpool.party.PartyUtil.PartyUtil;
import edu.kangwon.university.taxicarpool.party.PartyUtil.SearchVariant;
import edu.kangwon.university.taxicarpool.party.dto.PartyCreateRequestDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyUpdateRequestDTO;
import edu.kangwon.university.taxicarpool.party.partyException.PartyInvalidMaxParticipantException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException;
import edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartyService {

    @Value("${kakaomobility.api.key}")
    private String kakaoMobilityApiKey;
    private final PartyRepository partyRepository;
    private final PartyMapper partyMapper;
    private final MemberRepository memberRepository;
    private final ChattingService chattingService;
    private final FcmPushService fcmPushService;

    @Autowired
    PartyService(PartyRepository partyRepository,
        PartyMapper partyMapper,
        MemberRepository memberRepository, ChattingService chattingService,
        FcmPushService fcmPushService
    ) {
        this.partyRepository = partyRepository;
        this.partyMapper = partyMapper;
        this.memberRepository = memberRepository;
        this.chattingService = chattingService;
        this.fcmPushService = fcmPushService;
    }

    /**
     * 단일 파티 정보를 조회합니다.
     *
     * <p>삭제되지 않은 파티만 조회하며, 존재하지 않으면 예외를 던집니다.</p>
     *
     * @param partyId 조회할 파티 ID
     * @return 파티 응답 DTO
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         해당 파티가 존재하지 않거나 삭제된 경우
     */
    @Transactional(readOnly = true)
    public PartyResponseDTO getParty(Long partyId) {
        PartyEntity partyEntity = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        return partyMapper.convertToResponseDTO(partyEntity);
    }

    /**
     * 파티 목록을 페이지네이션으로 조회합니다.
     *
     * <p>삭제되지 않은 파티를 생성일 내림차순으로 반환합니다.</p>
     *
     * @param page 페이지 번호(0-base)
     * @param size 페이지 크기
     * @return 파티 응답 DTO의 페이지
     */
    @Transactional(readOnly = true)
    public Page<PartyResponseDTO> getPartyList(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, "createdAt"));
        Page<PartyEntity> partyEntities = partyRepository.findAllByIsDeletedFalse(pageable);
        return partyEntities.map(partyMapper::convertToResponseDTO);
    }

    /**
     * 사용자 입력(출발지/도착지/출발시간)을 기준으로 커스텀 필터된 파티 목록을 조회합니다.
     *
     * <p>세 그룹(출발지, 도착지, 출발시간) 중 최소 1개 이상은 필수이며,
     * 둘 이상 누락 시 예외가 발생합니다. 과거 출발시간은 허용하지 않습니다.</p>
     *
     * @param userDepartureLng 사용자 출발지 경도(x)
     * @param userDepartureLat 사용자 출발지 위도(y)
     * @param userDestinationLng 사용자 도착지 경도(x)
     * @param userDestinationLat 사용자 도착지 위도(y)
     * @param userDepartureTime 사용자 출발 시간(현재 이후)
     * @param page 페이지 번호(0-base)
     * @param size 페이지 크기
     * @return 조건에 부합하는 파티 응답 DTO의 페이지
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyGetCustomException
     *         입력 조합이 유효하지 않거나 출발시간이 과거인 경우
     */
    @Transactional(readOnly = true)
    public Page<PartyResponseDTO> getCustomPartyList(
        Double userDepartureLng,
        Double userDepartureLat,
        Double userDestinationLng,
        Double userDestinationLat,
        LocalDateTime userDepartureTime,
        Integer page, Integer size) {

        PartySearchFilter f = new PartySearchFilter(
            userDepartureLng, userDepartureLat,
            userDestinationLng, userDestinationLat,
            userDepartureTime
        );

        PartyUtil.validateSearchFilter(f);
        SearchVariant variant = PartyUtil.toSearchVariant(f);

        Pageable pageable = PageRequest.of(page, size);

        Page<PartyEntity> entities = switch (variant) {
            case ALL -> partyRepository.findCustomPartyList(
                f.getDepLng(), f.getDepLat(),
                f.getDstLng(), f.getDstLat(),
                f.getDepTime(), pageable
            );
            case NO_DEPARTURE -> partyRepository.findCustomPartyList(
                f.getDstLng(), f.getDstLat(),
                f.getDepTime(), pageable
            );
            case NO_DESTINATION -> partyRepository.findCustomPartyList(
                f.getDepLng(), f.getDepLat(),
                f.getDepTime(), pageable
            );
            case NO_TIME -> partyRepository.findCustomPartyList(
                f.getDepLng(), f.getDepLat(),
                f.getDstLng(), f.getDstLat(),
                pageable
            );
        };

        return entities.map(partyMapper::convertToResponseDTO);

    }

    /**
     * 파티를 생성합니다.
     *
     * <p>호스트 멤버 ID를 파티에 설정하고, 해당 멤버를 초기 참가자로 등록합니다.</p>
     *
     * @param createRequestDTO 파티 생성 요청 DTO
     * @param CreatorMemberId 파티 생성자(호스트) 멤버 ID
     * @return 생성된 파티의 응답 DTO
     * @throws java.lang.IllegalArgumentException 호스트 멤버 ID가 null인 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         호스트 멤버가 존재하지 않는 경우
     */
    @Transactional
    public PartyResponseDTO createParty(PartyCreateRequestDTO createRequestDTO,
        Long CreatorMemberId) {

        PartyEntity partyEntity = partyMapper.convertToEntity(createRequestDTO);

        if (CreatorMemberId != null) {
            partyEntity.setHostMemberId(
                CreatorMemberId);
        } else {
            throw new IllegalArgumentException("파티방을 만든 멤버의 Id가 null임.");
        }

        MemberEntity member = memberRepository.findById(CreatorMemberId)
            .orElseThrow(() -> new MemberNotFoundException("파티방을 만든 멤버가 존재하지 않습니다."));
        partyEntity.getMemberEntities().add(member);

        partyEntity.setCurrentParticipantCount(1);

        PartyEntity savedPartyEntity = partyRepository.save(partyEntity);
        return partyMapper.convertToResponseDTO(savedPartyEntity);
    }

    /**
     * 파티 정보를 수정합니다.
     *
     * <p>호스트만 수정할 수 있으며, 현재 인원보다 작은 최대 인원으로는 설정할 수 없습니다.</p>
     *
     * @param partyId 파티 ID
     * @param memberId 요청자 멤버 ID(호스트)
     * @param updateRequestDTO 파티 수정 요청 DTO
     * @return 수정된 파티 응답 DTO
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException
     *         호스트가 아닌 사용자가 수정하려는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyInvalidMaxParticipantException
     *         현재 인원보다 작은 최대 인원을 설정한 경우
     */
    @Transactional
    public PartyResponseDTO updateParty(Long partyId, Long memberId,
        PartyUpdateRequestDTO updateRequestDTO) {
        PartyEntity existingPartyEntity = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));

        if (!existingPartyEntity.getHostMemberId().equals(memberId)) {
            throw new UnauthorizedHostAccessException("호스트만 수정할 수 있습니다.");
        }

        if(existingPartyEntity.getCurrentParticipantCount() > updateRequestDTO.getMaxParticipantCount()) {
            throw new PartyInvalidMaxParticipantException("현재 참여 인원보다 작은 최대 인원으로 설정할 수 없습니다.");
        }

        partyMapper.convertToEntityByUpdate(existingPartyEntity, updateRequestDTO);

        PartyEntity savedPartyEntity = partyRepository.save(existingPartyEntity);
        return partyMapper.convertToResponseDTO(savedPartyEntity);
    }

    /**
     * 파티를 삭제(소프트 딜리트)합니다.
     *
     * <p>호스트만 삭제할 수 있습니다.</p>
     *
     * @param partyId 파티 ID
     * @param memberId 요청자 멤버 ID(호스트)
     * @return 삭제 결과 메시지 및 삭제된 파티 ID를 담은 맵
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException
     *         호스트가 아닌 사용자가 삭제하려는 경우
     */
    @Transactional
    public Map<String, Object> deleteParty(Long partyId, Long memberId) {
        PartyEntity partyEntity = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        if (!partyEntity.getHostMemberId().equals(memberId)) {
            throw new UnauthorizedHostAccessException("호스트만 삭제할 수 있습니다.");
        }

        // 알림 대상: 남아있는 파티원(요청자/호스트 제외)
        List<Long> targetIds = partyEntity.getMemberEntities().stream()
            .map(MemberEntity::getId)
            .filter(id -> !id.equals(memberId))
            .toList();

        partyEntity.setDeleted(true);

        // FCM 푸시 발송
        if (!targetIds.isEmpty()) {
            PushMessageDTO msg = PushMessageDTO.builder()
                .title("파티가 삭제되었습니다")
                .body("호스트가 파티를 삭제했어요.")
                .type("PARTY_DELETED")
                // 메시지 타입이 notification이지만 프론트의 딥링크/라우팅에서 사용하기 유용해서 data도 세팅
                .data(Map.of("partyId", String.valueOf(partyId)))
                .build();
            fcmPushService.sendPushToUsers(targetIds, msg);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "파티가 성공적으로 삭제되었습니다.");
        response.put("deletedPartyId", partyId);

        return response;
    }

    /**
     * 파티에 참가합니다.
     *
     * <p>이미 삭제된 파티, 이미 참가한 멤버, 정원이 꽉 찬 경우는 허용되지 않습니다.
     * 참가에 성공하면 시스템 입장 메시지를 생성합니다.</p>
     *
     * @param partyId 파티 ID
     * @param memberId 참가 멤버 ID
     * @return 갱신된 파티 응답 DTO
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyAlreadyDeletedException
     *         파티가 이미 삭제된 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         멤버가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberAlreadyInPartyException
     *         이미 파티에 속한 멤버인 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyFullException
     *         파티 정원이 가득 찬 경우
     */
    @Transactional
    public PartyResponseDTO joinParty(Long partyId, Long memberId) {
        PartyEntity party = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다."));

        party.join(member);

        PartyEntity saved = partyRepository.save(party);
        chattingService.createSystemMessage(saved, member, MessageType.ENTER);
        return partyMapper.convertToResponseDTO(saved);
    }

    /**
     * 파티에서 탈퇴합니다.
     *
     * <p>마지막 인원이 탈퇴하면 파티는 삭제 처리됩니다. 호스트가 탈퇴하는 경우,
     * 남아있는 첫 번째 멤버가 새 호스트로 승격됩니다. 탈퇴에 성공하면 시스템 퇴장 메시지를 생성합니다.</p>
     *
     * @param partyId 파티 ID
     * @param memberId 탈퇴 멤버 ID
     * @return 갱신된 파티 응답 DTO
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         멤버가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException
     *         해당 파티에 속하지 않은 멤버인 경우
     */
    @Transactional
    public PartyResponseDTO leaveParty(Long partyId, Long memberId) {
        PartyEntity party = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));
        MemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("해당 멤버가 존재하지 않습니다."));

        party.leave(memberId);

        PartyEntity saved = partyRepository.save(party);
        chattingService.createSystemMessage(party, member, MessageType.LEAVE);
        return partyMapper.convertToResponseDTO(saved);
    }

    /**
     * 특정 멤버가 속한 활성 파티 목록을 조회합니다.
     *
     * @param memberId 멤버 ID
     * @return 멤버가 참여 중인 활성 파티 응답 DTO 목록
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         멤버가 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public List<PartyResponseDTO> getMyParties(Long memberId) {

        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException("해당 멤버가 존재하지 않습니다: " + memberId);
        }

        List<PartyEntity> activeParties = partyRepository.findAllActivePartiesByMemberId(memberId);

        return activeParties.stream()
            .map(partyMapper::convertToResponseDTO)
            .toList();
    }

    /**
     * 카카오모빌리티 길찾기 API의 예측 요금을 이용해 파티의 절감 금액을 계산하고 각 멤버의 누적 절감액에 반영합니다.
     *
     * <p>호스트만 실행할 수 있으며, 한 번 계산이 완료된 파티는 재계산할 수 없습니다.
     * 출발/도착 좌표와 출발 시간이 유효해야 하며, 외부 API 호출/파싱 실패 시 예외가 발생합니다.</p>
     *
     * @param partyId 파티 ID
     * @param requesterId 요청자(호스트) 멤버 ID
     * @return 절감 계산 결과(참여 인원, 출발시간, 원/목적지, 총 요금, 1인당 부담액, 1인당 절감액 등)
     * @throws edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException
     *         파티가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException
     *         호스트가 아닌 사용자가 요청한 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.SavingsAlreadyCalculatedException
     *         이미 절감 계산이 완료된 경우
     * @throws java.lang.IllegalArgumentException 출발/도착 좌표가 없거나 범위를 벗어난 경우
     * @throws edu.kangwon.university.taxicarpool.party.partyException.KakaoApiException
     *         외부 API 호출 실패/응답 오류/파싱 실패 등
     * @throws edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException
     *         파티 참여 인원이 0명인 경우
     */
    @Transactional
    public Map<String, Object> calculateSavings(Long partyId, Long requesterId) {
        // 1) 파티 조회 & 권한/상태 검사
        PartyEntity party = partyRepository.findByIdAndIsDeletedFalse(partyId)
            .orElseThrow(() -> new PartyNotFoundException("해당 파티가 존재하지 않습니다."));

        PartyUtil.assertHostAndRecalc(party, requesterId);

        // 2) 좌표 검증 & 문자열 변환
        double[] coords = PartyUtil.getValidatedCoords(party);
        double sx = coords[0], sy = coords[1], ex = coords[2], ey = coords[3];
        String[] od = PartyUtil.toOriginDestination(sx, sy, ex, ey);
        String origin = od[0], destination = od[1];

        // 3) 출발 시각 보정/포맷
        LocalDateTime depTime = PartyUtil.ensureFutureDeparture(party.getStartDateTime());
        String departureTime = PartyUtil.formatDeparture(depTime);

        // 4) 외부 API 호출 (카카오 모빌리티)
        String url = PartyUtil.buildFutureDirectionsUrl(origin, destination, departureTime);
        String body = PartyUtil.fetchKakaoDirectionsJson(url, kakaoMobilityApiKey);
        long totalTaxiFare = PartyUtil.extractTaxiFare(body);

        // 5) 참여 인원/절감액 계산
        List<MemberEntity> members = party.getMemberEntities();
        int participants = PartyUtil.ensureParticipants(members);
        long[] shares = PartyUtil.calcShares(totalTaxiFare, participants);
        long eachShare = shares[0];
        long savingPerMember = shares[1];

        // 6) 멤버 누적 절감액 반영
        for (MemberEntity m : members) {
            m.addToTotalSavedAmount(savingPerMember);
        }
        memberRepository.saveAll(members);

        // 7) 파티 상태 갱신
        party.setSavingsCalculated(true);
        partyRepository.save(party);

        // 8) 응답 생성
        Map<String, Object> result = new HashMap<>();
        result.put("partyId", partyId);
        result.put("participants", participants);
        result.put("departure_time", departureTime);
        result.put("origin", origin);
        result.put("destination", destination);
        result.put("totalTaxiFare", totalTaxiFare);
        result.put("eachShare", eachShare);
        result.put("savingPerMember", savingPerMember);
        return result;
    }


}
