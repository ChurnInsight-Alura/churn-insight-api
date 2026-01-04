package com.alura.churnnsight.dto.creation;

import java.time.LocalDate;

public record DataCreateCustomer(
        String customerId,
        String geography,
        Integer gender,
        LocalDate birthDate,
        LocalDate createdAt
) {
}
