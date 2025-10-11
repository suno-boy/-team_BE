package edu.kangwon.university.taxicarpool.profanity;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ProfanityService
 * - 금칙어 사전 로딩 (classpath: profanity.txt)
 * - 문자열 정규화 (특수문자 제거, leet 치환)
 * - 포함 여부 탐지
 * - 마스킹 처리
 *
 * 운영에서는 DB/관리페이지 연동, 캐시/핫리로드 방식으로 확장 가능
 */
@Service
public class ProfanityService {

    private static final String DEFAULT_DICT_PATH = "profanity.txt";

    private final Set<String> blacklist;

    private static final Map<Character, Character> LEET = Map.of(
        '1','l', '!','i', '3','e', '4','a', '@','a',
        '5','s', '7','t', '0','o', '$','s'
    );

    private static final Set<Character> IGNORE_CHARS = Set.of(
        'ㅡ', '-', '_', '~'
    );

    private static final Pattern NON_KR_EN_NUM = Pattern.compile("[^0-9A-Za-z가-힣ㄱ-ㅎㅏ-ㅣ]");

    public ProfanityService() {
        this.blacklist = loadDefaultBlacklist();
    }

    /**
     * 금칙어 포함 여부 검사
     * @param value 검사할 문자열
     * @param allowlist 해당 필드에서 허용할 단어
     * @return true면 금칙어 포함
     */
    public boolean contains(String value, Set<String> allowlist) {
        if (value == null) return false;
        String norm = normalize(value);

        for (String bad : blacklist) {
            if (bad == null || bad.isBlank()) continue;
            if (allowlist != null && allowlist.contains(bad)) continue;

            String badNorm = normalize(bad);
            if (badNorm.isEmpty()) continue;

            if (norm.contains(badNorm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 변형(띄어쓰기/특수문자/leet/'ㅡ' 삽입 등)을 정규화로 흡수한 뒤,
     * 원문 내 대응 구간만 *로 마스킹한다.
     *  - 예: "ㅅㅡ ㅂ", "c!b@l", "씨--발" 등
     */
    public String maskSmart(String value) {
        if (value == null || value.isBlank()) return value;

        // 1) 원문 문자별로 "정규화에 기여하는지" 플래그와 해당 문자의 정규화 결과를 만든다.
        //    - IGNORE_CHARS 및 NON_KR_EN_NUM로 제거되는 문자는 '정규화 비기여'로 표기
        char[] original = value.toCharArray();
        String lowered = value.toLowerCase(Locale.ROOT);

        // 정규화 결과(문자열)
        StringBuilder normBuf = new StringBuilder(lowered.length());
        // 원문 인덱스 -> 정규화 인덱스의 매핑(정규화 비기여 문자는 -1)
        int[] mapToNorm = new int[original.length];
        Arrays.fill(mapToNorm, -1);

        for (int i = 0; i < original.length; i++) {
            char c = lowered.charAt(i);

            // NON_KR_EN_NUM로 제거될 문자거나, IGNORE_CHARS에 포함되면 스킵
            if (IGNORE_CHARS.contains(c) || NON_KR_EN_NUM.matcher(Character.toString(c)).find()) {
                continue;
            }
            char mapped = LEET.getOrDefault(c, c);
            mapToNorm[i] = normBuf.length();
            normBuf.append(mapped);
        }

        String norm = normBuf.toString();
        if (norm.isEmpty()) return value;

        // 2) 정규화 문자열에서 금칙어 매칭 영역을 모두 찾아, 원문 인덱스 집합으로 역매핑
        boolean[] maskFlags = new boolean[original.length];

        for (String bad : blacklist) {
            if (bad.isBlank()) continue;
            String badNorm = normalize(bad); // 동일한 규칙으로 정규화
            if (badNorm.isEmpty()) continue;

            int from = 0;
            while (true) {
                int hit = norm.indexOf(badNorm, from);
                if (hit < 0) break;

                int hitEnd = hit + badNorm.length() - 1;

                // 3) 정규화 인덱스 [hit..hitEnd]를 생산한 원문 인덱스들을 * 마킹
                for (int i = 0; i < mapToNorm.length; i++) {
                    int ni = mapToNorm[i];
                    if (ni >= 0 && ni >= hit && ni <= hitEnd) {
                        maskFlags[i] = true;
                    }
                }

                from = hit + 1;
            }
        }

        // 4) maskFlags가 true인 원문 문자만 *로 치환하여 반환
        StringBuilder out = new StringBuilder(original.length);
        for (int i = 0; i < original.length; i++) {
            out.append(maskFlags[i] ? '*' : original[i]);
        }
        return out.toString();
    }

    /**
     * 문자열 정규화
     * 1) 소문자화
     * 2) 특수문자/공백 제거
     * 3) leet 문자 치환
     */
    public String normalize(String s) {
        String lowered = s.toLowerCase(Locale.ROOT);
        String stripped = NON_KR_EN_NUM.matcher(lowered).replaceAll("");
        StringBuilder sb = new StringBuilder(stripped.length());
        for (char c : stripped.toCharArray()) {
            if (IGNORE_CHARS.contains(c)) continue;
            sb.append(LEET.getOrDefault(c, c));
        }
        return sb.toString();
    }

    /**
     * profanity.txt에서 금칙어 로딩
     * 없으면 기본 리스트 사용
     */
    private Set<String> loadDefaultBlacklist() {
        try {
            var res = new ClassPathResource(DEFAULT_DICT_PATH);
            if (!res.exists()) {
                return defaultSet();
            }
            String text = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            Set<String> set = new HashSet<>();
            for (String line : text.split("\\R")) {
                String w = line.trim();
                if (!w.isEmpty() && !w.startsWith("#")) {
                    set.add(w);
                }
            }
            return set;
        } catch (Exception e) {
            return defaultSet();
        }
    }

    private Set<String> defaultSet() {
        return new HashSet<>(List.of("씨발","ㅅㅂ","병신","좆","개새","개색","fuck","bitch","ㅈ같"));
    }
}
