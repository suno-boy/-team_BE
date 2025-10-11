package edu.kangwon.university.taxicarpool.chatting;

import edu.kangwon.university.taxicarpool.chatting.dto.MessageResponseDTO;
import edu.kangwon.university.taxicarpool.chatting.dto.NotificationRequestDTO;
import edu.kangwon.university.taxicarpool.chatting.dto.NotificationResponseDTO;
import edu.kangwon.university.taxicarpool.chatting.dto.ParticipantResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chatting", description = "채팅 메시지 조회·참가자 조회·공지사항 수정 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("api/party/{partyId}")
public class ChattingController {

    private final ChattingService chattingService;

    public ChattingController(ChattingService chattingService) {
        this.chattingService = chattingService;
    }

    @Operation(
        summary = "과거 메세지 조회",
        description = "특정 파티의 과거 메세지 기록을 가져옵니다.\n"
            + "afterMessageId=3 이면 4부터 오름차순으로 조회 되며, "
            + "afterMessageId를 포함하지 않는 경우 전체 메세지가 오름차순으로 조회됩니다."
            + "maxResults는 가져올 메세지의 최대 개수입니다. 기본값은 20입니다."
    )
    @GetMapping("/messages")
    public ResponseEntity<List<MessageResponseDTO>> getMessageHistory(
        @Parameter(description = "조회할 파티 ID") @PathVariable Long partyId,
        @Parameter(description = "기준이 되는 메세지 ID")
        @RequestParam(required = false) Long afterMessageId,
        @Parameter(description = "최대 메시지 개수")
        @RequestParam(required = false, defaultValue = "20") int maxResults) {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();

        List<MessageResponseDTO> messages = chattingService.getMessageHistory(partyId, memberId,
            afterMessageId, maxResults);

        return ResponseEntity.ok(messages);
    }

    @Operation(
        summary = "참가자 목록 조회",
        description = "특정 파티의 참가자 목록을 가져옵니다."
    )
    @GetMapping("/participants")
    public ResponseEntity<List<ParticipantResponseDTO>> getParticipants(
        @Parameter(description = "조회할 파티 ID", required = true) @PathVariable Long partyId
    ) {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();
        List<ParticipantResponseDTO> list = chattingService.getParticipants(partyId, memberId);
        return ResponseEntity.ok(list);
    }

    @Operation(
        summary = "공지사항 수정",
        description = "파티의 공지사항(notification)을 호스트만 수정할 수 있습니다."
    )
    @PutMapping("/notification")
    public ResponseEntity<NotificationResponseDTO> updateNotification(
        @Parameter(description = "수정할 파티 ID", required = true) @PathVariable Long partyId,
        @RequestBody @Valid NotificationRequestDTO request
    ) {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();

        NotificationResponseDTO response = chattingService
            .updateNotification(partyId, memberId, request.getNotification());

        return ResponseEntity.ok(response);
    }

}
