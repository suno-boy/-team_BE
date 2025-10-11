package edu.kangwon.university.taxicarpool.profanity;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO 바인딩 시점에 문자열에 비속어가 포함되었는지 검사.
 * - 빈 문자열/NULL은 통과(다른 @NotBlank로 관리)
 * - allowlist는 해당 필드에서만 예외 처리
 */
public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    @Autowired
    private ProfanityService profanityService;

    private Set<String> allowlist;

    @Override
    public void initialize(NoProfanity ann) {
        this.allowlist = new HashSet<>(Arrays.asList(ann.allowlist()));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (!StringUtils.hasText(value)) return true;

        return !profanityService.contains(value, allowlist);
    }
}
