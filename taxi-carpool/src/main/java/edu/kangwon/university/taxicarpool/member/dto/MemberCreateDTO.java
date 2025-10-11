package edu.kangwon.university.taxicarpool.member.dto;

import edu.kangwon.university.taxicarpool.member.Gender;
import edu.kangwon.university.taxicarpool.member.validation.EmailValid;
import edu.kangwon.university.taxicarpool.member.validation.NicknameValid;
import edu.kangwon.university.taxicarpool.member.validation.PasswordValid;
import edu.kangwon.university.taxicarpool.profanity.NoProfanity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateDTO {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @EmailValid
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @PasswordValid
    private String password;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @NicknameValid
    @NoProfanity(message = "닉네임에 비속어가 포함되어 있습니다.")
    private String nickname;

    @NotNull(message = "성별은 필수 선택 항목입니다.")
    private Gender gender;
}
