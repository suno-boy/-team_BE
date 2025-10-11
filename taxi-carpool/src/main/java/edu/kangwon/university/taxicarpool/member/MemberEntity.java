package edu.kangwon.university.taxicarpool.member;

import edu.kangwon.university.taxicarpool.chatting.MessageEntity;
import edu.kangwon.university.taxicarpool.party.PartyEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String email;

    @NotNull
    private String password;

    @NotNull
    @Column(unique = true)
    private String nickname;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @ManyToMany(mappedBy = "memberEntities")
    private List<PartyEntity> parties = new ArrayList<>();

    @OneToMany(mappedBy = "sender")
    private List<MessageEntity> sentMessages = new ArrayList<>();

    @Column(name = "total_saved_amount", nullable = false)
    private long totalSavedAmount = 0L;

    @Column(nullable = false)
    private int tokenVersion = 0;

    public MemberEntity(String email, String password, String nickname, Gender gender) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.gender = gender;
    }

    public void setPassword(@NotNull String password) {
        this.password = password;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public void setEmail(@NotNull String email) {
        this.email = email;
    }

    public void setGender(
        @NotNull Gender gender) {
        this.gender = gender;
    }

    /** 절감 금액을 누적하는 편의 메서드 */
    public void addToTotalSavedAmount(long amountToAdd) {
        if (amountToAdd < 0) {
            throw new IllegalArgumentException("누적 절감 금액은 음수가 될 수 없습니다.");
        }
        this.totalSavedAmount += amountToAdd;
    }
}