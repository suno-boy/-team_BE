package edu.kangwon.university.taxicarpool.auth;

import edu.kangwon.university.taxicarpool.auth.authException.AuthenticationFailedException;
import edu.kangwon.university.taxicarpool.auth.authException.TokenExpiredException;
import edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException;
import edu.kangwon.university.taxicarpool.auth.dto.LoginRequest;
import edu.kangwon.university.taxicarpool.auth.dto.LoginResponse;
import edu.kangwon.university.taxicarpool.auth.dto.RefreshRequestDTO;
import edu.kangwon.university.taxicarpool.auth.dto.RefreshResponseDTO;
import edu.kangwon.university.taxicarpool.email.EmailVerificationService;
import edu.kangwon.university.taxicarpool.email.exception.EmailVerificationNotFoundException;
import edu.kangwon.university.taxicarpool.fcm.FcmTokenRepository;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.MemberService;
import edu.kangwon.university.taxicarpool.member.dto.MemberCreateDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberDetailDTO;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 회원가입을 수행합니다.
     *
     * <p>사전에 이메일 인증이 완료되었는지 확인한 뒤, 회원 생성 로직을 위임합니다.</p>
     *
     * @param request 회원 생성 요청 DTO
     * @return 생성된 회원의 상세 DTO
     * @throws edu.kangwon.university.taxicarpool.email.exception.EmailVerificationNotFoundException
     *         이메일 인증이 완료되지 않은 경우
     */
    public MemberDetailDTO signUp(MemberCreateDTO request) {

        // 이메일 인증여부 확인
        if(!emailVerificationService.isEmailVerified(request.getEmail())) {
            throw new EmailVerificationNotFoundException("이메일 인증을 먼저 해주세요.");
        }

        return memberService.createMember(request);
    }

    /**
     * 로그인 처리 후 액세스/리프레시 토큰을 발급합니다.
     *
     * <p>이메일/비밀번호 검증 후, 멤버의 토큰 버전을 증가시켜 신규 액세스 토큰에 반영하고
     * 리프레시 토큰을 DB에 저장(있으면 갱신)합니다.</p>
     *
     * @param request 로그인 요청 DTO(이메일/비밀번호)
     * @return 액세스 토큰, 리프레시 토큰, 이메일을 담은 응답 DTO
     * @throws edu.kangwon.university.taxicarpool.auth.authException.AuthenticationFailedException
     *         비밀번호가 일치하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         이메일에 해당하는 회원을 찾을 수 없는 경우
     */
    public LoginResponse login(LoginRequest request) {

        MemberEntity member = memberService.getMemberEntityByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new AuthenticationFailedException("비밀번호가 올바르지 않습니다.");
        }

        member.setTokenVersion(member.getTokenVersion() + 1);
        memberRepository.save(member);

        String accessToken = jwtUtil.generateAccessToken(member.getId(), member.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(member.getId());

        LocalDateTime refreshExpiry = LocalDateTime.now().plusDays(7);

        // DB에 리프래쉬 토큰 저장(이미 있으면 업데이트)
        Optional<RefreshTokenEntity> optionalToken = refreshTokenRepository.findByMember(member);
        if (optionalToken.isPresent()) {
            RefreshTokenEntity tokenEntity = optionalToken.get();
            tokenEntity.updateRefreshToken(refreshToken, refreshExpiry);
            refreshTokenRepository.save(tokenEntity);
        } else {
            RefreshTokenEntity newToken = new RefreshTokenEntity(member, refreshToken,
                refreshExpiry);
            refreshTokenRepository.save(newToken);
        }

        return new LoginResponse(accessToken, refreshToken, member.getEmail());
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰을 재발급합니다.
     *
     * <p>요청된 리프레시 토큰을 DB에서 검증하고, 만료 여부를 확인한 뒤
     * 멤버의 현재 토큰 버전으로 액세스 토큰을 생성합니다.</p>
     *
     * @param request 액세스 토큰 재발급 요청 DTO(리프레시 토큰 포함)
     * @return 새 액세스 토큰과 기존 리프레시 토큰을 담은 응답 DTO
     * @throws edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException
     *         리프레시 토큰이 존재하지 않거나 회원 정보를 찾을 수 없는 경우
     * @throws edu.kangwon.university.taxicarpool.auth.authException.TokenExpiredException
     *         리프레시 토큰이 만료된 경우
     */
    public RefreshResponseDTO refresh(RefreshRequestDTO request) {
        
        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByRefreshToken(
                request.getRefreshToken())
            .orElseThrow(() -> new TokenInvalidException("리프래쉬 토큰이 만료되었습니다. 다시 로그인해주세요."));
        
        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("리프래쉬 토큰이 만료되었습니다. 다시 로그인해주세요.");
        }

        // 새 액세스 토큰 발급
        Long id = tokenEntity.getMember().getId();
        MemberEntity fresh = memberRepository.findById(id)
            .orElseThrow(() -> new TokenInvalidException("회원 정보를 찾을 수 없습니다."));

        String newAccessToken = jwtUtil.generateAccessToken(id, fresh.getTokenVersion());

        return new RefreshResponseDTO(newAccessToken, tokenEntity.getRefreshToken());
    }

    /**
     * 로그아웃 처리로 리프레시 토큰을 무효화합니다.
     *
     * <p>DB에서 리프레시 토큰을 조회해 만료 시간을 현재 시각으로 갱신합니다
     * (또는 삭제 로직으로 대체 가능).</p>
     *
     * @param refreshToken 무효화할 리프레시 토큰
     * @throws edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException
     *         유효한 리프레시 토큰을 찾을 수 없는 경우(재로그인 필요)
     */
    @Transactional
    public void logout(String refreshToken) {

        Optional<RefreshTokenEntity> optionalToken = refreshTokenRepository.findByRefreshToken(refreshToken);
        if (optionalToken.isPresent()) {
            RefreshTokenEntity tokenEntity = optionalToken.get();

            // 리프레쉬 토큰 무효화
            tokenEntity.setExpiryDate(LocalDateTime.now());
            refreshTokenRepository.save(tokenEntity);

            // FCM 토큰 폐기
            Long userId = tokenEntity.getMember().getId();
            fcmTokenRepository.revokeAllTokensByUserId(userId);
        } else {
            throw new TokenInvalidException("재로그인 후 로그아웃 해주세요.");
        }
    }

}
