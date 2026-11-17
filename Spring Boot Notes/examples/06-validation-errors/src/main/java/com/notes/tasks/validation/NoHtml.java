package com.notes.tasks.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A custom constraint: the text must not contain HTML tags (a first defence against stored XSS, file 11). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoHtmlValidator.class)        // the class that does the check
public @interface NoHtml {
    String message() default "must not contain HTML tags";
    Class<?>[] groups() default {};                      // required by the spec
    Class<? extends Payload>[] payload() default {};     // required by the spec
}
