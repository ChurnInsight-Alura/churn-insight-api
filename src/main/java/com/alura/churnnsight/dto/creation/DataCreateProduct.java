package com.alura.churnnsight.dto.creation;

import jakarta.validation.constraints.NotBlank;

public record DataCreateProduct(
        @NotBlank(message = "El name no puede estar vacío")
        String name
) {
}
