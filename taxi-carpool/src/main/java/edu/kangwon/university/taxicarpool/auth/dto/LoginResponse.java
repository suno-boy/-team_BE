package edu.kangwon.university.taxicarpool.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor      // JSON 직렬화/역직렬화 호환성
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;        // JWT 액세스 토큰 (2시간)
    private String refreshToken; // 리프래쉬 토큰 (1주)
    private String email;        // 사용자 식별 정보
}
