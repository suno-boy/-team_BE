package edu.kangwon.university.taxicarpool.fcm;

import edu.kangwon.university.taxicarpool.fcm.dto.FcmTokenUpsertRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FCM Token", description = "FCM 토큰 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    /**
     * FCM 토큰 등록/갱신 (업서트)
     */
    @Operation(
        summary = "FCM 토큰 등록/갱신",
        description = "단일 기기 정책에 따라, 요청 사용자에 대한 기존 활성 토큰을 모두 무효화한 뒤 새 FCM 토큰 1개만 저장합니다."
    )
    @PostMapping("/token")
    public ResponseEntity<Void> upsertFcmToken(
        @Valid @RequestBody FcmTokenUpsertRequestDTO requestDTO
    ) {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();

        fcmTokenService.upsertFcmToken(memberId, requestDTO);

        return ResponseEntity.ok().build();
    }


}
