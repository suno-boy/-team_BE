package edu.kangwon.university.taxicarpool.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일이 공백일 수 없습니다")
    @Email
    private String email;

    @NotBlank(message = "패스워드가 공백일 수 없습니다")
    private String password;
}