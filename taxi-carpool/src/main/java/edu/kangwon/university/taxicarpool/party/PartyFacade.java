package edu.kangwon.university.taxicarpool.party;

import edu.kangwon.university.taxicarpool.party.dto.PartyResponseDTO;
import edu.kangwon.university.taxicarpool.party.partyException.PartyFullException;
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

    public PartyResponseDTO joinParty(Long partyId, Long memberId) {

        final String lockKey = "party:join:" + partyId;

        RLock lock = redissonClient.getFairLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, 20, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new PartyFullException("현재 파티 참여 요청이 많습니다. 잠시 후에 시도해주세요.");
            }

            return partyService.joinParty(partyId, memberId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("파티 참여 락 대기 중 인터럽트가 발생했습니다.", e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}