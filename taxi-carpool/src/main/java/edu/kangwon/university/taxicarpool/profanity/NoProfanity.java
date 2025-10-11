package edu.kangwon.university.taxicarpool.profanity;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoProfanityValidator.class)
public @interface NoProfanity {
    String message() default "비속어가 포함되어 있습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * 특수한 이유로 허용할 단어(화이트리스트)
     */
    String[] allowlist() default {};
}