package com.alura.churnnsight.service.helpers;

import java.time.LocalDate;

public class BirthDateEstimator {
    private BirthDateEstimator() {}

    public static LocalDate fromAge(Integer age) {
        if (age == null || age < 0) {
            throw new IllegalArgumentException("Age is required to compute birthDate");
        }
        int year = LocalDate.now().minusYears(age).getYear();
        return LocalDate.of(year, 1, 1);
    }
}
