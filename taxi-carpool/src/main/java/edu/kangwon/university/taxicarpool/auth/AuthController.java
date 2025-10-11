package edu.kangwon.university.taxicarpool.auth;

import edu.kangwon.university.taxicarpool.auth.dto.LoginRequest;
import edu.kangwon.university.taxicarpool.auth.dto.LoginResponse;
import edu.kangwon.university.taxicarpool.auth.dto.LogoutRequestDTO;
import edu.kangwon.university.taxicarpool.auth.dto.RefreshRequestDTO;
import edu.kangwon.university.taxicarpool.auth.dto.RefreshResponseDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberCreateDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberDetailDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "회원가입·로그인·토큰 갱신·로그아웃 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
        summary = "회원가입",
        description = "새로운 회원을 등록하고 회원 정보를 반환합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<MemberDetailDTO> signUp(
        @Validated
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "가입할 회원 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = MemberCreateDTO.class))
        )
        @RequestBody MemberCreateDTO memberCreateDTO
    ) {
        MemberDetailDTO response = authService.signUp(memberCreateDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인하고 Access/Refresh 토큰, email을 반환합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Validated
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "로그인 요청 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
        )
        @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "토큰 갱신",
        description = "Refresh 토큰으로 새로운 Access 토큰을 발급받습니다."
    )
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDTO> refresh(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "리프레시 요청 정보. 리프레시 토큰을 담아주세요. 그러면 엑세스 토큰 새로 발급해드립니다.",
            required = true,
            content = @Content(schema = @Schema(implementation = RefreshRequestDTO.class))
        )
        @RequestBody RefreshRequestDTO request
    ) {
        RefreshResponseDTO response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "로그아웃",
        description = "리프레쉬 토큰을 무효화 후 서버에서도 세션을 만료합니다.(엑세스는 stateless라 무효화 불가능)"
    )
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
        @Validated
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "로그아웃 요청 정보 (refresh토큰)",
            required = true,
            content = @Content(schema = @Schema(implementation = LogoutRequestDTO.class))
        )
        @RequestBody LogoutRequestDTO logoutRequest
    ) {
        authService.logout(logoutRequest.getRefreshToken());
        return ResponseEntity.ok("로그아웃 완료");
    }
}
