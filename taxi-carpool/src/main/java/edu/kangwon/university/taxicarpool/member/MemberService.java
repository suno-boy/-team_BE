package edu.kangwon.university.taxicarpool.member;

import edu.kangwon.university.taxicarpool.auth.RefreshTokenRepository;
import edu.kangwon.university.taxicarpool.fcm.FcmTokenRepository;
import edu.kangwon.university.taxicarpool.member.dto.MemberCreateDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberDetailDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberPublicDTO;
import edu.kangwon.university.taxicarpool.member.dto.MemberUpdateDTO;
import edu.kangwon.university.taxicarpool.member.exception.DuplicatedEmailException;
import edu.kangwon.university.taxicarpool.member.exception.DuplicatedNicknameException;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberMapper memberMapper;
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 회원을 생성합니다.
     *
     * <p>이메일/닉네임의 중복을 검증하고 비밀번호를 인코딩한 뒤 저장합니다.</p>
     *
     * @param memberCreateDTO 회원 생성 요청 DTO
     * @return 생성된 회원의 상세 DTO
     * @throws edu.kangwon.university.taxicarpool.member.exception.DuplicatedEmailException
     *         이미 사용 중인 이메일인 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.DuplicatedNicknameException
     *         이미 사용 중인 닉네임인 경우
     */
    @Transactional
    public MemberDetailDTO createMember(MemberCreateDTO memberCreateDTO) {

        if (memberRepository.existsByEmail(memberCreateDTO.getEmail())) {
            throw new DuplicatedEmailException("이미 사용 중인 이메일입니다: " + memberCreateDTO.getEmail());
        }

        if (memberRepository.existsByNickname(memberCreateDTO.getNickname())) {
            throw new DuplicatedNicknameException(
                "이미 사용 중인 닉네임입니다: " + memberCreateDTO.getNickname());
        }

        MemberEntity entity = memberMapper.toEntity(memberCreateDTO);

        String encodedPassword = passwordEncoder.encode(memberCreateDTO.getPassword());
        entity.setPassword(encodedPassword);

        MemberEntity saved = memberRepository.save(entity);
        return memberMapper.toDetailDTO(saved);
    }

    /**
     * 회원 정보를 수정합니다.
     *
     * <p>닉네임 변경 시 중복 여부를 검증하고, 비밀번호는 인코딩하여 저장합니다.
     * 닉네임/비밀번호 중 최소 하나는 제공되어야 합니다.</p>
     *
     * @param memberId 수정 대상 회원 ID
     * @param updateDTO 회원 수정 요청 DTO(새 닉네임/새 비밀번호)
     * @return 수정된 회원의 상세 DTO
     * @throws java.lang.IllegalArgumentException 수정할 항목이 없는 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         회원을 찾을 수 없는 경우
     * @throws edu.kangwon.university.taxicarpool.member.exception.DuplicatedNicknameException
     *         닉네임이 중복된 경우
     */
    @Transactional
    public MemberDetailDTO updateMember(Long memberId, MemberUpdateDTO updateDTO) {
        if (updateDTO.isEmpty()) {
            throw new IllegalArgumentException("수정할 닉네임이나 비밀번호 중 하나 이상은 반드시 제공되어야 합니다.");
        }

        MemberEntity existedEntity = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + memberId));

        if (updateDTO.getNewNickname() != null && !updateDTO.getNewNickname()
            .equals(existedEntity.getNickname())) {

            if (memberRepository.existsByNickname(updateDTO.getNewNickname())) {
                throw new DuplicatedNicknameException(
                    "이미 사용 중인 닉네임입니다: " + updateDTO.getNewNickname());
            }
            existedEntity.setNickname(updateDTO.getNewNickname());
        }

        if (updateDTO.getNewPassword() != null && !updateDTO.getNewPassword().isBlank()) {
            String encodedPassword = passwordEncoder.encode(updateDTO.getNewPassword());
            existedEntity.setPassword(encodedPassword);
        }

        MemberEntity updated = memberRepository.save(existedEntity);
        return memberMapper.toDetailDTO(updated);
    }

    /**
     * 회원을 삭제합니다.
     *
     * <p>회원의 리프레시 토큰이 존재하면 함께 삭제합니다.</p>
     *
     * @param memberId 삭제할 회원 ID
     * @return 삭제된 회원의 상세 DTO(삭제 직전 스냅샷)
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         회원을 찾을 수 없는 경우
     */
    @Transactional
    public MemberDetailDTO deleteMember(Long memberId) {
        MemberEntity entity = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + memberId));

        refreshTokenRepository.findByMember(entity).ifPresent(refreshTokenRepository::delete);

        fcmTokenRepository.deleteAllByUserId(memberId);

        memberRepository.delete(entity);
        return memberMapper.toDetailDTO(entity);
    }

    /**
     * 회원의 공개 정보를 조회합니다.
     *
     * <p>닉네임 등 공개 가능한 최소 정보만 반환합니다.</p>
     *
     * @param memberId 회원 ID
     * @return 회원 공개 DTO
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         회원을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public MemberPublicDTO getMemberById(Long memberId) {

        MemberEntity entity = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + memberId));

        return memberMapper.toPublicDTO(entity);
    }

    /**
     * 회원의 상세 정보를 조회합니다.
     *
     * <p>이메일, 닉네임, 성별, 누적 절감액 등을 포함합니다.</p>
     *
     * @param memberId 회원 ID
     * @return 회원 상세 DTO
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         회원을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public MemberDetailDTO getDetailMember(Long memberId) {
        MemberEntity entity = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다: " + memberId));
        return memberMapper.toDetailDTO(entity);
    }

    /**
     * 이메일로 회원 엔티티를 조회합니다.
     *
     * <p>인증/인가 등 내부 로직에서 엔티티가 필요한 경우에 사용합니다.</p>
     *
     * @param email 회원 이메일
     * @return 회원 엔티티
     * @throws edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException
     *         해당 이메일의 회원이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public MemberEntity getMemberEntityByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new MemberNotFoundException("존재하지 않는 이메일입니다: " + email));
    }

}
