package edu.kangwon.university.taxicarpool.party.PartyUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kangwon.university.taxicarpool.member.MemberEntity;
import edu.kangwon.university.taxicarpool.party.PartyEntity;
import edu.kangwon.university.taxicarpool.party.partyException.KakaoApiException;
import edu.kangwon.university.taxicarpool.party.partyException.MemberNotInPartyException;
import edu.kangwon.university.taxicarpool.party.partyException.PartyGetCustomException;
import edu.kangwon.university.taxicarpool.party.partyException.SavingsAlreadyCalculatedException;
import edu.kangwon.university.taxicarpool.party.partyException.UnauthorizedHostAccessException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public final class PartyUtil {

    private PartyUtil() {}

    private static final DateTimeFormatter DEPT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * 커스텀 검색 필터 유효성 검증
     */
    public static void validateSearchFilter(PartySearchFilter f) {
        if (f.hasTime() && !f.getDepTime().isAfter(LocalDateTime.now())) {
            throw new PartyGetCustomException("출발 시간은 현재 시간보다 이후여야 합니다.");
        }

        int missing = 0;
        if (!f.hasDeparture())   missing++;
        if (!f.hasDestination()) missing++;
        if (!f.hasTime())        missing++;

        if (missing >= 2) {
            throw new PartyGetCustomException("출발지, 도착지, 출발시간 중 최소 2개는 제공되어야 합니다.");
        }
    }

    /**
     * 필터로부터 검색 변형 결정
     */
    public static SearchVariant toSearchVariant(PartySearchFilter f) {
        if (f.hasDeparture() && f.hasDestination() && f.hasTime()) return SearchVariant.ALL;
        if (!f.hasDeparture())   return SearchVariant.NO_DEPARTURE;
        if (!f.hasDestination()) return SearchVariant.NO_DESTINATION;
        return SearchVariant.NO_TIME;
    }

    /**
     * 좌표 유효성 검증(대략적 한반도 범위)
     */
    public static void validateCoordinate(double x, double y, String label) {
        if (x < 124 || x > 132) {
            throw new IllegalArgumentException(label + " 경도(x)가 한반도 범위를 벗어났습니다: x=" + x);
        }
        if (y < 33 || y > 39) {
            throw new IllegalArgumentException(label + " 위도(y)가 한반도 범위를 벗어났습니다: y=" + y);
        }
    }

    /** 호스트 권한 및 재계산 여부 검사 */
    public static void assertHostAndRecalc(PartyEntity party, Long requesterId) {
        if (party.getHostMemberId() == null || !party.getHostMemberId().equals(requesterId)) {
            throw new UnauthorizedHostAccessException("호스트만 절감 금액 계산을 수행할 수 있습니다.");
        }
        if (party.isSavingsCalculated()) {
            throw new SavingsAlreadyCalculatedException("해당 파티(" + party.getId() + ")는 이미 절감 계산이 완료되었습니다.");
        }
    }

    /** 파티의 출발/도착 좌표 추출 + 좌표 유효성 검사 */
    public static double[] getValidatedCoords(PartyEntity party) {
        if (party.getStartPlace() == null || party.getEndPlace() == null) {
            throw new IllegalArgumentException("출발/도착 좌표가 없습니다.");
        }
        double sx = party.getStartPlace().getX();
        double sy = party.getStartPlace().getY();
        double ex = party.getEndPlace().getX();
        double ey = party.getEndPlace().getY();

        validateCoordinate(sx, sy, "출발지");
        validateCoordinate(ex, ey, "도착지");

        return new double[]{sx, sy, ex, ey};
    }

    /** 출발/도착 문자열(origin, destination) 생성 */
    public static String[] toOriginDestination(double sx, double sy, double ex, double ey) {
        return new String[]{sx + "," + sy, ex + "," + ey};
    }

    /** 과거 시간이면 now+2분로 보정한 출발 시각 반환 */
    public static LocalDateTime ensureFutureDeparture(LocalDateTime start) {
        LocalDateTime base = (start != null) ? start : LocalDateTime.now().plusMinutes(2);
        if (!base.isAfter(LocalDateTime.now())) {
            base = LocalDateTime.now().plusMinutes(2);
        }
        return base;
    }

    /** 출발 시각 포맷(yyyyMMddHHmm) */
    public static String formatDeparture(LocalDateTime depTime) {
        return depTime.format(DEPT_FMT);
    }

    /** 카카오 길찾기(미래 경로) URL 생성 */
    public static String buildFutureDirectionsUrl(String origin, String destination, String departureTime) {
        return UriComponentsBuilder
            .fromHttpUrl("https://apis-navi.kakaomobility.com/v1/future/directions")
            .queryParam("origin", origin)
            .queryParam("destination", destination)
            .queryParam("departure_time", departureTime)
            .build(true)
            .toUriString();
    }

    /** 카카오 API 호출(JSON 반환) */
    public static String fetchKakaoDirectionsJson(String url, String kakaoApiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        headers.set("Content-Type", "application/json");

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new KakaoApiException("카카오 API 호출 실패: 성공(2xx) 응답이 아님. URL=" + url);
        }
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new KakaoApiException("카카오 API 호출 실패: 응답 본문이 비어있음. URL=" + url);
        }
        return body;
    }

    /** JSON 본문에서 taxi 요금 추출 */
    public static long extractTaxiFare(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new KakaoApiException("카카오 API 응답 오류: 경로 정보가 비어있습니다.");
            }

            JsonNode fare = routes.get(0).path("summary").path("fare");
            if (fare.isMissingNode()) {
                throw new KakaoApiException("카카오 API 응답 오류: 요금 정보가 없습니다.");
            }

            long totalTaxiFare = fare.path("taxi").asLong(0L);
            if (totalTaxiFare <= 0L) {
                throw new KakaoApiException("카카오 API 응답 오류: 유효한 택시 요금을 가져오지 못했습니다.");
            }
            return totalTaxiFare;
        } catch (IOException e) {
            throw new KakaoApiException("카카오 모빌리티 API 응답 파싱 실패: JSON 변환 불가", e);
        }
    }

    /** 참여 인원수 확보(0이면 예외) */
    public static int ensureParticipants(List<MemberEntity> members) {
        int participants = (members != null) ? members.size() : 0;
        if (participants <= 0) {
            throw new MemberNotInPartyException("파티 참여 인원이 0명입니다.");
        }
        return participants;
    }

    /** 1인당 부담액, 1인당 절감액 계산 */
    public static long[] calcShares(long totalTaxiFare, int participants) {
        long eachShare = totalTaxiFare / participants;
        long savingPerMember = totalTaxiFare - eachShare;
        return new long[]{eachShare, savingPerMember};
    }
}
