package com.ads.adshub.util.validation;

import java.lang.annotation.Documented;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = DateValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDate {

    String message() default "Invalid date format. Expected format: YYYY-MM-DD with valid range.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

