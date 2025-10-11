package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.fcm.FcmPushService;
import edu.kangwon.university.taxicarpool.fcm.dto.PushMessageDTO;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartySchedulerService {

    private final PartyRepository partyRepository;
    private final FcmPushService fcmPushService;

    /**
     * 매분 0초에 실행되어 출발 10분 전인 파티의 멤버들에게 알림을 보냅니다.
     * (cron = "초 분 시 일 월 요일")
     */
    @Scheduled(cron = "0 * * * * *") // 매분 0초에 실행
    @Transactional
    public void sendDepartureReminders() {
        // 1. 10분 ~ 11분 뒤 출발하는 파티 조회
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime after10Minutes = now.plusMinutes(10);
        LocalDateTime before11Minutes = now.plusMinutes(11);

        List<PartyEntity> parties = partyRepository.findPartiesForDepartureReminder(after10Minutes, before11Minutes);

        for (PartyEntity party : parties) {
            // 2. 파티 멤버 ID 목록 추출
            List<Long> memberIds = party.getMemberEntities().stream()
                .map(MemberEntity::getId)
                .collect(Collectors.toList());

            if (memberIds.isEmpty()) {
                continue; // 멤버가 없으면 건너뛰기
            }

            // 3. 푸시 메시지 생성
            String formattedDepartureTime = party.getStartDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            PushMessageDTO pushMessage = PushMessageDTO.builder()
                .title("곧 택시가 출발해요! 🚕")
                .body(String.format("'%s' 파티가 10분 뒤(%s) 출발합니다. 늦지 않게 준비해주세요!",
                    party.getName(), formattedDepartureTime))
                .type("DEPARTURE_REMINDER")
                .build();
            pushMessage.getData().put("partyId", String.valueOf(party.getId()));

            // 4. FCM 푸시 발송
            fcmPushService.sendPushToUsers(memberIds, pushMessage);

            // 5. 알림 발송 완료 플래그 업데이트 (중복 방지)
            party.setDepartureNotificationSent(true);
            partyRepository.save(party);
        }
    }
}