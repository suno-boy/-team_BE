package edu.kangwon.university.taxicarpool.email;

import edu.kangwon.university.taxicarpool.email.exception.EmailSendFailedException;
import edu.kangwon.university.taxicarpool.email.exception.EmailVerificationNotFoundException;
import edu.kangwon.university.taxicarpool.email.exception.ExpiredVerificationCodeException;
import edu.kangwon.university.taxicarpool.email.exception.InvalidVerificationCodeException;
import edu.kangwon.university.taxicarpool.member.MemberRepository;
import edu.kangwon.university.taxicarpool.member.exception.DuplicatedEmailException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    @Value("${email.address}")
    private String emailAddress;

    @Value("${email.name}")
    private String emailName;

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailVerificationService(MemberRepository memberRepository,
        EmailVerificationRepository emailVerificationRepository,
        JavaMailSender javaMailSender) {
        this.memberRepository = memberRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.javaMailSender = javaMailSender;
    }

    /**
     * 인증 코드를 생성하여 이메일로 전송합니다.
     *
     * <p>이미 가입된 이메일이면 전송하지 않고 예외를 던지며,
     * 발급된 코드는 10분 뒤 만료되도록 저장합니다. 전송 실패 시 저장한 코드도 롤백(삭제)합니다.</p>
     *
     * @param email 수신자 이메일
     * @throws edu.kangwon.university.taxicarpool.member.exception.DuplicatedEmailException
     *         이미 가입된 이메일인 경우
     * @throws edu.kangwon.university.taxicarpool.email.exception.EmailSendFailedException
     *         메일 전송에 실패한 경우
     * @throws java.lang.Exception 메일 전송 과정에서 발생할 수 있는 일반 예외 전파
     */
    public void sendCode(String email) throws Exception {

        if (memberRepository.existsByEmail(email)) {
            throw new DuplicatedEmailException("이미 사용 중인 이메일입니다: " + email);
        }

        String code = generateCode();

        EmailVerificationEntity entity = new EmailVerificationEntity(email, code,
            LocalDateTime.now().plusMinutes(10));

        emailVerificationRepository.findByEmail(email).ifPresent(
            emailVerificationRepository::delete);

        emailVerificationRepository.save(entity);

        try {
            sendEmail(email, code);
        } catch (Exception e) {
            // 이메일 전송 실패 시 저장된 인증 코드 삭제
            emailVerificationRepository.delete(entity);
            throw new EmailSendFailedException("이메일 전송에 실패하였습니다.");
        }

    }

    /**
     * 사용자가 입력한 인증 코드를 검증합니다.
     *
     * <p>코드가 일치하고 만료되지 않았다면 해당 이메일을 인증됨으로 표시합니다.</p>
     *
     * @param email 검증 대상 이메일
     * @param code  사용자가 입력한 6자리 코드
     * @throws edu.kangwon.university.taxicarpool.email.exception.EmailVerificationNotFoundException
     *         해당 이메일의 인증 정보가 존재하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.email.exception.InvalidVerificationCodeException
     *         인증 코드가 일치하지 않는 경우
     * @throws edu.kangwon.university.taxicarpool.email.exception.ExpiredVerificationCodeException
     *         인증 코드가 만료된 경우
     */
    public void verifyCode(String email, String code) {

        EmailVerificationEntity entity = emailVerificationRepository.findByEmail(email)
            .orElseThrow(() -> new EmailVerificationNotFoundException("인증 정보를 찾을 수 없습니다."));

        if (!entity.getVerificationCode().equals(code)) {
            throw new InvalidVerificationCodeException("인증 코드가 일치하지 않습니다.");
        }

        if (entity.isExpired()) {
            throw new ExpiredVerificationCodeException("인증 코드가 만료되었습니다.");
        }

        entity.setVerified(true);
        emailVerificationRepository.save(entity);

    }

    /**
     * 이메일이 인증 완료 상태인지 확인합니다.
     *
     * @param email 조회할 이메일
     * @return true: 인증됨, false: 미인증
     * @throws edu.kangwon.university.taxicarpool.email.exception.EmailVerificationNotFoundException
     *         해당 이메일의 인증 정보가 존재하지 않는 경우
     */
    public boolean isEmailVerified(String email) {
        EmailVerificationEntity entity = emailVerificationRepository.findByEmail(email)
            .orElseThrow(() -> new EmailVerificationNotFoundException("인증 정보를 찾을 수 없습니다."));
        return entity.isVerified();
    }

    /**
     * 6자리 난수 인증 코드를 생성합니다.
     *
     * <p>{@link java.security.SecureRandom} 기반으로 100000~999999 범위를 생성합니다.</p>
     *
     * @return 6자리 숫자 문자열 코드
     */
    private String generateCode() {
        Random random = new SecureRandom();
        int num = random.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(num);
    }

    /**
     * 실제 이메일을 전송합니다.
     *
     * <p>발신자 주소/이름은 설정값을 사용하며, 제목은 "인증코드 안내",
     * 본문은 "인증코드: {code}" 형식으로 전송합니다.</p>
     *
     * @param to 수신자 이메일
     * @param code 전송할 인증 코드
     * @throws java.lang.Exception 메일 메시지 구성/전송 과정에서 발생한 예외
     */
    private void sendEmail(String to, String code) throws Exception {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(new InternetAddress(emailAddress, emailName));
        helper.setTo(to);
        helper.setSubject("인증코드 안내");
        helper.setText("인증코드: " + code);

        javaMailSender.send(message);
    }
}
