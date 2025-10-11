package edu.kangwon.university.taxicarpool.member;

import edu.kangwon.university.taxicarpool.member.dto.MemberDetailDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberPublicDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "멤버 조회·수정·삭제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "타인 정보 조회", description = "ID로 타인의 정보를 가져옵니다.")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberPublicDTO> getPublicInfo(
        @Parameter(description = "조회할 멤버 ID", required = true) @PathVariable Long memberId) {
        MemberPublicDTO publicInfo = memberService.getMemberById(memberId);
        return ResponseEntity.ok(publicInfo);
    }

    @Operation(summary = "본인 정보 조회", description = "로그인된 본인 정보를 가져옵니다.")
    @GetMapping("/me")
    public ResponseEntity<MemberDetailDTO> getMyInfo() {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();
        MemberDetailDTO myInfo = memberService.getDetailMember(memberId);
        return ResponseEntity.ok(myInfo);
    }

    @Operation(summary = "본인 정보 수정", description = "로그인된 본인 정보를 수정합니다.\nnewPassword와 newNickname 중 최소 한 개는 포함되어야 합니다.")
    @PatchMapping("/me")
    public ResponseEntity<MemberDetailDTO> updateMember(
        @Parameter(description = "수정할 정보 DTO", required = true)
        @RequestBody @Valid MemberUpdateDTO updateDTO
    ) {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();
        MemberDetailDTO updated = memberService.updateMember(memberId, updateDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "회원 탈퇴", description = "로그인된 본인 정보를 삭제합니다. (탈퇴 진행)")
    @DeleteMapping("/me")
    public ResponseEntity<MemberDetailDTO> deleteMember() {
        Long memberId = (Long) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();
        MemberDetailDTO deleted = memberService.deleteMember(memberId);
        return ResponseEntity.ok(deleted);
    }
}
