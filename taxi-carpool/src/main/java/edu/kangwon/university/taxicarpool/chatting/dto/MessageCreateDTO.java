package edu.kangwon.university.taxicarpool.chatting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor              // JSON 역직렬화용
@AllArgsConstructor             // content 필드 한 개짜리 생성자
public class MessageCreateDTO {

    private String content;
}
