package edu.kangwon.university.taxicarpool.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LogoutRequestDTO {

    @NotBlank(message = "재로그인 후 로그아웃 해주세요.") // 토큰 없는 로그아웃 요청
    private String refreshToken;
}
