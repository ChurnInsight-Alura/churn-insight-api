package com.alura.churnnsight.model.enumeration;

import java.util.Arrays;

public enum InterventionPriority {
    HIGH,
    MEDIUM,
    LOW;

    public static InterventionPriority fromString(String value) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid priority: " + value)
                );
    }
}
