package edu.kangwon.university.taxicarpool.email;

import edu.kangwon.university.taxicarpool.email.dto.EmailSendRequest;
import edu.kangwon.university.taxicarpool.email.dto.EmailVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email Verification", description = "이메일 인증 코드 발송 및 검증 API")
@RestController
@RequestMapping("/api/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * 이메일 인증코드 발송
     */
    @Operation(
        summary = "인증 코드 발송",
        description = "요청한 이메일로 인증 코드를 발송합니다."
    )
    @PostMapping("/send")
    public ResponseEntity<String> sendCode(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "이메일 전송 요청 DTO",
            required = true,
            content = @Content(schema = @Schema(implementation = EmailSendRequest.class))
        )
        @RequestBody EmailSendRequest request) throws Exception {
        emailVerificationService.sendCode(request.getEmail());
        return ResponseEntity.ok("이메일 전송 완료");
    }

    /**
     * 인증코드 검증
     */
    @Operation(
        summary = "인증 코드 검증",
        description = "받은 인증 코드가 유효한지 검증합니다."
    )
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "인증 검증 요청 DTO",
            required = true,
            content = @Content(schema = @Schema(implementation = EmailVerifyRequest.class))
        )
        @RequestBody EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok("인증 성공");
    }
}
