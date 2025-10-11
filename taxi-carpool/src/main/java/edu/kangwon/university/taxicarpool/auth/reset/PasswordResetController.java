package edu.kangwon.university.taxicarpool.auth.reset;

import edu.kangwon.university.taxicarpool.auth.reset.dto.ResetRequest;
import edu.kangwon.university.taxicarpool.auth.reset.dto.SendLinkRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Password Reset", description = "비밀번호 재설정(링크 발송/1회용 토큰 사용) API")
@RestController
@RequestMapping("/api/password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "비밀번호 재설정 링크 발송", description = "입력한 이메일로 1회용 토큰이 포함된 링크를 보냅니다.")
    @PostMapping("/reset-link")
    public ResponseEntity<String> sendLink(@Valid @RequestBody SendLinkRequest request) {
        passwordResetService.sendResetLink(request.getEmail());
        return ResponseEntity.ok("비밀번호 재설정 안내 메일을 발송했습니다. 10분안에 변경해주세요.");
    }

    @Operation(summary = "비밀번호 재설정", description = "이메일 링크의 토큰으로 새 비밀번호로 변경합니다.")
    @PatchMapping("/reset")
    public ResponseEntity<String> reset(@Valid @RequestBody ResetRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }
}
