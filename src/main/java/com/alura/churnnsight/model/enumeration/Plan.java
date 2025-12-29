package com.alura.churnnsight.model.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Plan {
    BASIC("basic"),
    STANDARD("standard"),
    PREMIUM("premium");

    private final String value;

    Plan(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
    @JsonCreator
    public static Plan fromValue(String value){
        return  Arrays.stream(values())
                .filter(p->p.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(()-> new IllegalArgumentException("Plan invalido " + value));
    }
}
