package com.notes.tasks.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {
    private static final Pattern TAG = Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !TAG.matcher(value).find();   // null is "valid": combine with @NotBlank
    }
}
