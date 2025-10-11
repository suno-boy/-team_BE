package edu.kangwon.university.taxicarpool.fcm;

import com.google.firebase.messaging.MessagingErrorCode;
import edu.kangwon.university.taxicarpool.fcm.dto.FcmTokenUpsertRequestDTO;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final MemberRepository memberRepository;

    /**
     * FCM 토큰 등록/갱신 (업서트) 동일한 사용자의 동일한 플랫폼 토큰이 있으면 갱신, 없으면 새로 생성
     */
    @Transactional
    public void upsertFcmToken(Long userId, FcmTokenUpsertRequestDTO requestDTO) {
        // 동일 fcmToken 전역 폐기: 다른 사용자 소유 레코드도 모두 무효화
        revokeTokenByFcmToken(requestDTO.getFcmToken());
        // 단일 기기 강제: 기존 활성 토큰 전부 무효화
        fcmTokenRepository.revokeAllTokensByUserId(userId);

        MemberEntity member = memberRepository.findById(userId)
            .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + userId));

        FcmTokenEntity newToken = new FcmTokenEntity(
            member,
            requestDTO.getFcmToken(),
            requestDTO.getPlatform(),
            requestDTO.getDeviceId(),
            requestDTO.getAppVersion(),
            requestDTO.getBundleOrPackage()
        );
        fcmTokenRepository.save(newToken);
    }

    /**
     * 특정 FCM 토큰 폐기
     */
    @Transactional
    public void revokeTokenByFcmToken(String fcmToken) {
        fcmTokenRepository.revokeTokenByFcmToken(fcmToken);
    }

    /**
     * FCM 응답 오류에 따른 토큰 폐기 처리
     */
    @Transactional
    public void handleFcmError(String fcmToken, MessagingErrorCode errorCode) {
        // UNREGISTERED, INVALID_REGISTRATION_TOKEN 등의 오류 시 토큰 폐기
        if (shouldRevokeToken(errorCode)) {
            revokeTokenByFcmToken(fcmToken);
        }
    }

    /**
     * FCM 오류 코드에 따라 토큰 폐기 여부 결정
     */
    private boolean shouldRevokeToken(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED
            || errorCode == MessagingErrorCode.INVALID_ARGUMENT
            || errorCode == MessagingErrorCode.SENDER_ID_MISMATCH;
    }
}
