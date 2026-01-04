package com.alura.churnnsight.dto.creation;

public record DataCreateCustomerStatus(
    String customerId,
    Integer creditScore,
    Boolean isActiveMember
) {
}
