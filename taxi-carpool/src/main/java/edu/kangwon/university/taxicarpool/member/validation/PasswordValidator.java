package edu.kangwon.university.taxicarpool.member.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<PasswordValid, String> {

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-={}\\[\\]:\";'<>?,./]{8,20}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return PASSWORD_PATTERN.matcher(value).matches();
    }
}