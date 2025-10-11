package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.party.dto.PartyCreateRequestDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.dto.PartyUpdateRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Party", description = "카풀방 생성·조회·수정·삭제·참여 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/party")
@Validated
public class PartyController {

    private final PartyService partyService;

    @Autowired
    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @Operation(summary = "파티 조회", description = "ID로 특정 파티 정보를 가져옵니다.")
    @GetMapping("/{partyId}")
    public ResponseEntity<PartyResponseDTO> getParty(
        @Parameter(description = "조회할 파티 ID", required = true)
        @PathVariable("partyId") Long partyId
    ) {
        PartyResponseDTO partyResponse = partyService.getParty(partyId);
        return ResponseEntity.ok(partyResponse);
    }

    @Operation(summary = "파티 리스트 조회", description = "모든 파티를 페이지 단위로 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<PartyResponseDTO>> getPartyList(
        @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") Integer page,
        @Parameter(description = "페이지 크기 (최소 1)") @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") Integer size
    ) {
        Page<PartyResponseDTO> partyList = partyService.getPartyList(page, size);
        return ResponseEntity.ok(partyList);
    }

    @Operation(summary = "커스텀 파티 조회", description = "위치·시간 필터 적용 조회")
    @GetMapping("/custom")
    public ResponseEntity<Page<PartyResponseDTO>> getCustomPartyList(
        @Parameter(description = "출발지 경도") @RequestParam(required = false) Double userDepartureLng,
        @Parameter(description = "출발지 위도") @RequestParam(required = false) Double userDepartureLat,
        @Parameter(description = "도착지 경도") @RequestParam(required = false) Double userDestinationLng,
        @Parameter(description = "도착지 위도") @RequestParam(required = false) Double userDestinationLat,
        @Parameter(description = "출발 시간 (ISO 날짜시간)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime userDepartureTime,
        @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") Integer page,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.") Integer size
    ) {
        Page<PartyResponseDTO> partyList = partyService.getCustomPartyList(userDepartureLng,
            userDepartureLat,
            userDestinationLng, userDestinationLat,
            userDepartureTime, page, size);
        return ResponseEntity.ok(partyList);
    }

    @Operation(summary = "파티 생성", description = "새 카풀방을 만듭니다.")
    @PostMapping
    public ResponseEntity<PartyResponseDTO> createParty(
        @Parameter(description = "생성 요청 DTO", required = true,
            schema = @Schema(implementation = PartyCreateRequestDTO.class))
        @RequestBody @Valid PartyCreateRequestDTO createRequestDTO
    ) {
        Long memberId = (Long) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(partyService.createParty(createRequestDTO, memberId));
    }

    @Operation(summary = "파티 수정", description = "카풀방 정보를 업데이트합니다.")
    @PatchMapping("/{partyId}")
    public ResponseEntity<PartyResponseDTO> updateParty(
        @Parameter(description = "업데이트 요청 DTO", required = true,
            schema = @Schema(implementation = PartyUpdateRequestDTO.class))
        @RequestBody @Valid PartyUpdateRequestDTO updateRequestDTO,
        @Parameter(description = "파티 ID", required = true) @PathVariable Long partyId
    ) {
        Long memberId = (Long) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(partyService.updateParty(partyId, memberId, updateRequestDTO));
    }

    @Operation(summary = "파티 삭제", description = "카풀방을 삭제합니다.")
    @DeleteMapping("/{partyId}")
    public ResponseEntity<Map<String, Object>> deleteParty(
        @Parameter(description = "파티 ID", required = true) @PathVariable Long partyId
    ) {
        Long memberId = (Long) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(partyService.deleteParty(partyId, memberId));
    }

    @Operation(summary = "파티 참여", description = "멤버를 카풀방에 참여시킵니다.")
    @PostMapping("/{partyId}/join")
    public ResponseEntity<PartyResponseDTO> joinParty(
        @Parameter(description = "파티 ID", required = true) @PathVariable Long partyId
    ) {
        Long memberId = (Long) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(partyService.joinParty(partyId, memberId));
    }

    @Operation(summary = "파티 퇴장", description = "멤버를 카풀방에서 퇴장시킵니다.")
    @PostMapping("/{partyId}/leave")
    public ResponseEntity<PartyResponseDTO> leaveParty(
        @Parameter(description = "파티 ID", required = true) @PathVariable Long partyId
    ) {
        Long memberId = (Long) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        PartyResponseDTO result = partyService.leaveParty(partyId, memberId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "내 파티 목록 조회", description = "로그인한 사용자가 속한 모든 파티를 조회합니다.")
    @GetMapping("/my-parties")
    public ResponseEntity<List<PartyResponseDTO>> getMyParties() {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();
        List<PartyResponseDTO> myParties = partyService.getMyParties(memberId);
        return ResponseEntity.ok(myParties);
    }

    @Operation(
        summary = "파티 절감 금액 계산",
        description = "호스트가 완료된 파티에 대해 절감 금액을 계산하고, 각 멤버의 누적 절감액(totalSavedAmount)에 반영합니다."
    )
    @PostMapping("/{partyId}/savings")
    public ResponseEntity<Map<String, Object>> calculateSavings(
        @Parameter(description = "파티 ID", required = true) @PathVariable Long partyId
    ) {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> result = partyService.calculateSavings(partyId, memberId);
        return ResponseEntity.ok(result);
    }


}
