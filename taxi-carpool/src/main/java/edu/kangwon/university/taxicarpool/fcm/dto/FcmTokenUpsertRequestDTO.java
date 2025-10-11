package edu.kangwon.university.taxicarpool.fcm.dto;

import edu.kangwon.university.taxicarpool.fcm.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenUpsertRequestDTO {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String fcmToken;

    @NotNull(message = "플랫폼은 필수입니다.")
    private Platform platform;

    private String deviceId;

    private String appVersion;

    private String bundleOrPackage;
}
