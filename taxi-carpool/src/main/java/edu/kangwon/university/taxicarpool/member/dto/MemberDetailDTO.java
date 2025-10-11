package edu.kangwon.university.taxicarpool.member.dto;

import edu.kangwon.university.taxicarpool.member.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MemberDetailDTO {

    private Long id;
    private String email;
    private String nickname;
    private Gender gender;
    private long totalSavedAmount;
}
