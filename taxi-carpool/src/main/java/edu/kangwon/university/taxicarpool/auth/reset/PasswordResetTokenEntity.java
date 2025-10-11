package edu.kangwon.university.taxicarpool.auth.reset;

import edu.kangwon.university.taxicarpool.member.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token", indexes = {
    @Index(name = "idx_password_reset_token_jti", columnList = "jti", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String jti; // JWT ID(고유 식별자)

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MemberEntity member;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean used;  // 1회용 사용 여부

    private LocalDateTime usedAt;

    public PasswordResetTokenEntity(String jti, MemberEntity member, LocalDateTime expiresAt) {
        this.jti = jti;
        this.member = member;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }
}
