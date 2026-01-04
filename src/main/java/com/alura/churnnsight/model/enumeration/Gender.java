package com.alura.churnnsight.model.enumeration;

import java.util.Arrays;

public enum Gender {
    FEMALE(0),
    MALE(1);

    private final int code;

    Gender(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
    public static Gender fromCode(int code) {
        return Arrays.stream(values())
                .filter(g -> g.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid gender code: " + code)
                );
    }
}
