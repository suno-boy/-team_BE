package edu.kangwon.university.taxicarpool.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MemberPublicDTO {

    private Long id;
    private String nickname;
}
