package com.alura.churnnsight.dto.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClienteIn(
        @JsonProperty("RowNumber") Integer rowNumber,
        @JsonProperty("CustomerId") String customerId,
        @JsonProperty("Surname") String surname,
        @JsonProperty("CreditScore") Integer creditScore,
        @JsonProperty("Geography") String geography,
        @JsonProperty("Gender") String gender,
        @JsonProperty("Age") Integer age,
        @JsonProperty("Tenure") Integer tenure,
        @JsonProperty("Balance") Float balance,
        @JsonProperty("NumOfProducts")
        @JsonAlias({"numOfProducts", "NumOfProducts"})Integer numOfProducts,
        @JsonProperty("HasCrCard") Integer hasCrCard,
        @JsonProperty("IsActiveMember") Integer isActiveMember,
        @JsonProperty("EstimatedSalary") Float estimatedSalary
) {
}
