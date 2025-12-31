package com.ads.adshub.util.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;



public class DateValidator implements ConstraintValidator<ValidDate, LocalDate> {

    private static final Pattern DATE_PATTERN = 
        Pattern.compile("^(19|20)\\d{2}-(0[1-9]|1[0-2])-([0-2][0-9]|3[01])$");

    @Override
    public boolean isValid(LocalDate date, ConstraintValidatorContext context) {
        if (date == null) {
            return true; // Handle null separately with @NotNull if needed
        }

        // Check if date is within the valid range
        int year = date.getYear();
        if (year < 1900 || year > 2100) {
            return false;
        }

        try {
            // Validate the date format and ensure proper month-day combination
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateString = date.format(formatter);

            if (!DATE_PATTERN.matcher(dateString).matches()) {
                return false;
            }

            // Check leap year logic for February
            if (date.getMonthValue() == 2 && date.getDayOfMonth() > 29) {
                return false;
            }
            if (!date.isLeapYear() && date.getMonthValue() == 2 && date.getDayOfMonth() == 29) {
                return false;
            }

            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

