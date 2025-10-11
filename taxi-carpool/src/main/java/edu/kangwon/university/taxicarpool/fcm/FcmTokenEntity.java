package edu.kangwon.university.taxicarpool.fcm;

import edu.kangwon.university.taxicarpool.member.MemberEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity(name = "fcm_token")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "bundle_or_package")
    private String bundleOrPackage;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FcmTokenEntity(
        MemberEntity member,
        String fcmToken,
        Platform platform,
        String deviceId,
        String appVersion,
        String bundleOrPackage
    ) {
        this.member = member;
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.bundleOrPackage = bundleOrPackage;
        this.revoked = false;
    }

    public void updateToken(
        String fcmToken,
        String deviceId,
        String appVersion,
        String bundleOrPackage
    ) {
        this.fcmToken = fcmToken;
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.bundleOrPackage = bundleOrPackage;
        this.revoked = false;
    }

    public void revoke() {
        this.revoked = true;
    }
}
