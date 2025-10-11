package edu.kangwon.university.taxicarpool.auth.reset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendLinkRequest {

    @Schema(description = "비번 재설정 링크를 받을 이메일")
    @NotBlank
    @Email
    private String email;
}
