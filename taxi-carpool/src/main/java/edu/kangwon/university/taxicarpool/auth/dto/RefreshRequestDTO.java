package edu.kangwon.university.taxicarpool.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RefreshRequestDTO {

    @NotNull
    private String refreshToken;
}
