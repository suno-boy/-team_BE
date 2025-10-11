package edu.kangwon.university.taxicarpool.auth.reset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResetRequest {

    @Schema(description = "이메일 링크에 포함된 1회용 토큰")
    @NotBlank
    private String token;

    @Schema(description = "새 비밀번호")
    @NotBlank
    private String newPassword;
}
