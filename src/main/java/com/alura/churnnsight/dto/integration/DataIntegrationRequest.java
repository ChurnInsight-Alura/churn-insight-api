package com.alura.churnnsight.dto.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DataIntegrationRequest(
        @Valid
        @NotNull(message = "El cliente es requerido")
        ClienteIn cliente,
        @Valid
        List<TransaccionIn> transacciones,
        @Valid
        List<SesionIn> sesiones
) {
}
