package edu.kangwon.university.taxicarpool.auth.reset;

import edu.kangwon.university.taxicarpool.auth.JwtUtil;
import edu.kangwon.university.taxicarpool.auth.authException.TokenExpiredException;
import edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException;
import edu.kangwon.university.taxicarpool.email.exception.EmailSendFailedException;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.MemberService;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PasswordResetService {

    private final MemberService memberService;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${email.address}")
    private String emailAddress;

    @Value("${email.name}")
    private String emailName;

    // 프론트 리셋 페이지 베이스 URL
    @Value("${app.password-reset.base-url:http://localhost:3000/reset-password}")
    private String resetBaseUrl;

    // 1회용 토큰 유효시간(분)
    private static final long RESET_TOKEN_MINUTES = 10;

    public PasswordResetService(MemberService memberService,
        PasswordResetTokenRepository tokenRepository,
        JavaMailSender mailSender,
        JwtUtil jwtUtil,
        PasswordEncoder passwordEncoder) {
        this.memberService = memberService;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 비밀번호 재설정 링크를 이메일로 발송합니다.
     *
     * <p>요청한 이메일의 존재 여부는 외부에 드러나지 않도록 동일한 응답을 지향합니다.
     * 내부적으로 jti를 생성해 DB에 저장하고, jti가 포함된 단기 만료 JWT를 링크에 부착해 전송합니다.</p>
     *
     * @param email 재설정 링크를 받을 이메일
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException 이메일에 해당하는
     *                                                                                     회원이 없을 때
     *                                                                                     (구현에 따라
     *                                                                                     내부에서 처리될
     *                                                                                     수 있음)
     * @throws edu.kangwon.university.taxicarpool.email.exception.EmailSendFailedException 메일 전송에
     *                                                                                     실패한 경우
     */
    @Transactional
    public void sendResetLink(String email) {
        MemberEntity member = memberService.getMemberEntityByEmail(email);
        if (member == null) {
            // 예외처리는 getMemberEntityByEmail에서 구현중
            return;
        }

        String jti = JwtUtil.newJti();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES);
        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity(jti, member, expiresAt);
        tokenRepository.save(tokenEntity);

        String jwt = jwtUtil.generatePasswordResetToken(
            member.getId(), jti, 1000L * 60 * RESET_TOKEN_MINUTES
        );

        String link = resetBaseUrl + "?token=" + jwt;

        sendResetEmail(email, link);
    }

    /**
     * 1회용 재설정 토큰을 검증한 뒤 새 비밀번호로 변경합니다.
     *
     * <p>JWT 서명/만료 검증 → DB의 jti 조회로 1회성/만료/대상 일치 여부를 확인한 뒤
     * 비밀번호를 인코딩하여 저장하고 토큰을 사용 처리합니다.</p>
     *
     * @param token       비밀번호 재설정 JWT(내부에 jti 포함)
     * @param newPassword 새 비밀번호(평문)
     * @throws edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException jti
     *                                                                                     누락/불일치/이미
     *                                                                                     사용된 토큰/대상
     *                                                                                     불일치 등
     *                                                                                     유효하지 않은
     *                                                                                     토큰인 경우
     * @throws edu.kangwon.university.taxicarpool.auth.authException.TokenExpiredException 토큰이 만료된
     *                                                                                     경우
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 1) 토큰 기본 검증(서명/만료)
        var claims = jwtUtil.parseClaims(token);
        Long memberId = Long.valueOf(claims.getSubject());
        String jti = String.valueOf(claims.get("jti"));
        if (jti == null || jti.isBlank()) {
            throw new TokenInvalidException("유효하지 않은 토큰(jti 누락)");
        }

        // 2) DB에서 jti 조회 → 1회성/만료 검증 + 토큰 대상 일치 확인
        PasswordResetTokenEntity tokenEntity = tokenRepository.findByJti(jti)
            .orElseThrow(() -> new TokenInvalidException("유효하지 않은 토큰(jti 없음)"));

        if (tokenEntity.isUsed()) {
            throw new TokenInvalidException("이미 사용된 토큰입니다.");
        }
        if (tokenEntity.isExpired()) {
            throw new TokenExpiredException("토큰이 만료되었습니다. 다시 요청해 주세요.");
        }
        if (!tokenEntity.getMember().getId().equals(memberId)) {
            throw new TokenInvalidException("토큰 대상 정보가 일치하지 않습니다.");
        }

        // 3) 비밀번호 변경
        MemberEntity member = tokenEntity.getMember();
        member.setPassword(passwordEncoder.encode(newPassword));

        // 4) 토큰 사용 처리(1회용)
        tokenEntity.markUsed();

        // JPA flush는 @Transactional 종료 시점에 자동으로 반영되어서 save 불필요
    }

    /**
     * 비밀번호 재설정 안내 메일을 전송합니다.
     *
     * <p>제목은 "[강원대 택시카풀] 비밀번호 재설정 안내"이며,
     * 본문에는 재설정 링크와 유효시간(분)을 포함합니다.</p>
     *
     * @param to        수신자 이메일
     * @param resetLink 재설정 링크(URL 쿼리스트링에 token 포함)
     * @throws edu.kangwon.university.taxicarpool.email.exception.EmailSendFailedException 메일 구성/전송
     *                                                                                     중 예외가 발생한
     *                                                                                     경우
     */
    private void sendResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(new InternetAddress(emailAddress, emailName));
            helper.setTo(to);
            helper.setSubject("[강원대 택시카풀] 비밀번호 재설정 안내");
            helper.setText(
                "아래 링크를 통해 비밀번호를 재설정하세요.\n\n" + resetLink +
                    "\n\n링크 유효시간: " + RESET_TOKEN_MINUTES + "분",
                false
            );
            mailSender.send(message);
        } catch (Exception e) {

            throw new EmailSendFailedException("비밀번호 재설정 메일 전송 실패");
        }
    }
}
