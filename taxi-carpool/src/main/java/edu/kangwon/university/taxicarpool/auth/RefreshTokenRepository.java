package edu.kangwon.university.taxicarpool.auth;

import edu.kangwon.university.taxicarpool.member.MemberEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByMember(MemberEntity member);

    Optional<RefreshTokenEntity> findByRefreshToken(String refreshToken);
}
