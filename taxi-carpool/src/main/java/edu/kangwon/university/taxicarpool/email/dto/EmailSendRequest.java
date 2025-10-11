package edu.kangwon.university.taxicarpool.email.dto;

import edu.kangwon.university.taxicarpool.member.validation.EmailValid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmailSendRequest {

    @NotNull
    @EmailValid
    private String email;
}
