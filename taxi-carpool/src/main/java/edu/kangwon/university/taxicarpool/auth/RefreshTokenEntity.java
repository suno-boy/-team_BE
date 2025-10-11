package edu.kangwon.university.taxicarpool.auth;

import edu.kangwon.university.taxicarpool.member.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    private String refreshToken;

    private LocalDateTime expiryDate;

    public RefreshTokenEntity(MemberEntity member, String refreshToken, LocalDateTime expiryDate) {
        this.member = member;
        this.refreshToken = refreshToken;
        this.expiryDate = expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime expiryDate) {
        this.refreshToken = refreshToken;
        this.expiryDate = expiryDate;
    }
}
