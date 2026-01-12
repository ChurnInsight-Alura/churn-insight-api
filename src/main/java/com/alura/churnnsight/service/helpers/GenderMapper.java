package com.alura.churnnsight.service.helpers;

import com.alura.churnnsight.model.enumeration.Gender;

public final class GenderMapper {
    private GenderMapper() {}

    public static Gender parseGender(String value) {
        if (value == null) return Gender.Female; // default defensivo

        // Soporta "Male"/"Female" (Data) y "MALE"/"FEMALE"
        String v = value.trim().toLowerCase();
        return switch (v) {
            case "male", "m" -> Gender.Male;
            case "female", "f" -> Gender.Female;
            default -> throw new IllegalArgumentException("Invalid gender: " + value);
        };
    }
}
