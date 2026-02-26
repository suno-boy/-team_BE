package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.chatting.exception.InvalidMessageTypeException;
import edu.kangwon.university.taxicarpool.member.exception.MemberNotFoundException;
import edu.kangwon.university.taxicarpool.party.dto.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.partyException.MemberAlreadyInPartyException;
import edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyAlreadyDeletedException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyFullException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyGenderMismatchException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyLockInterruptedException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyNotFoundException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyFacade {

    private final RedissonClient redissonClient;
    private final PartyService partyService;

    @CircuitBreaker(name = "redis-circuit", fallbackMethod = "joinPartyFailFast")
    public PartyResponseDTO joinParty(Long partyId, Long memberId) {

        final String lockKey = "party:join:" + partyId;

        RLock lock = redissonClient.getFairLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(3, 20, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new PartyFullException("현재 파티 참여 요청이 많습니다. 잠시 후에 시도해주세요.");
            }

            return partyService.joinParty(partyId, memberId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PartyLockInterruptedException("서버 처리 중 지연이 발생했습니다. 잠시 후 다시 시도해주세요.", e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public PartyResponseDTO joinPartyFailFast(Long partyId, Long memberId, Throwable t) {

        if (t instanceof CallNotPermittedException) {
            log.error("🚨 서킷 브레이커 Open 상태! (Fail-Fast 차단)");
            throw new PartyServiceUnavailableException("현재 접속자가 많아 시스템에 일시적인 장애가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }

        if (t instanceof PartyGenderMismatchException ||
            t instanceof PartyFullException ||
            t instanceof MemberAlreadyInPartyException ||
            t instanceof PartyNotFoundException ||
            t instanceof PartyAlreadyDeletedException ||
            t instanceof MemberNotInPartyException ||
            t instanceof MemberNotFoundException ||
            t instanceof InvalidMessageTypeException) {

            throw (RuntimeException) t;
        }

        log.error("🚨 인프라 장애 발생! Fail-Fast로 차단합니다. 에러명: {}, 메시지: {}", t.getClass().getSimpleName(), t.getMessage());
        throw new PartyServiceUnavailableException("현재 시스템에 일시적인 장애가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
}