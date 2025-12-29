package com.alura.churnnsight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public record DataMakePrediction(
        @NotBlank(message = "CustomerId es obligatorio")
        @JsonProperty("CustomerId")
        String customerId,

        @NotNull(message = "CreditScore es obligatorio")
        @Min(value = 0, message = "CreditScore no puede ser negativo")
        @JsonProperty("CreditScore")
        Integer creditScore,

        @NotBlank(message = "Geography es obligatorio")
        @JsonProperty("Geography")
        String geography,

        @NotNull(message = "Gender es obligatorio (0 o 1)")
        @Min(0) @Max(1)
        @JsonProperty("Gender")
        Integer gender,

        @NotNull(message = "Age es obligatorio")
        @Min(value = 0, message = "Age no puede ser negativa")
        @JsonProperty("Age")
        Integer age,

        @NotNull(message = "Tenure es obligatorio")
        @Min(value = 0, message = "Tenure no puede ser negativo")
        @JsonProperty("Tenure")
        Integer tenure,

        @NotNull(message = "Balance es obligatorio")
        @DecimalMin(value = "0.0", message = "Balance no puede ser negativo")
        @JsonProperty("Balance")
        Double balance,

        @NotNull(message = "NumOfProducts es obligatorio")
        @Min(value = 0, message = "NumOfProducts no puede ser negativo")
        @JsonProperty("NumOfProducts")
        Integer numOfProducts,

        @NotNull(message = "IsActiveMember es obligatorio (0 o 1)")
        @Min(0) @Max(1)
        @JsonProperty("IsActiveMember")
        Integer isActiveMember
) {}