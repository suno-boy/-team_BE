package edu.kangwon.university.taxicarpool.chatting;

import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.party.PartyEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    @NotNull
    private PartyEntity party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private MemberEntity sender;

    @Column(name = "content", length = 1000)
    @NotNull
    private String content;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    @NotNull
    private MessageType type;

    public MessageEntity(PartyEntity party, MemberEntity sender, String content, MessageType type) {
        this.party = party;
        this.sender = sender;
        this.content = content;
        this.type = type;
    }

    public void setParty(PartyEntity party) {
        this.party = party;
    }
}
