package com.alura.churnnsight.dto;

import com.alura.churnnsight.model.enumeration.Plan;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;


public record DataMakePrediction(
        @NotNull(message = "El id de cliente es obligatorio")
        Long id,

        @JsonProperty("tiempo_contrato_meses")
        @PositiveOrZero(message = "El tiempo de contrato no puede ser negativo")
        Integer tiempoContratoMeses,

        @NotNull(message = "El plan (BASIC, STANDARD, PREMIUM) es obligatorio")
        Plan plan,

        @JsonProperty("uso_mensual")
        @DecimalMin(value = "0.0", message = "El uso mensual debe ser 0 o superior")
        Float usoMensual,

        @JsonProperty("retrasos_pago_90d")
        @PositiveOrZero(message = "Los retrasos no pueden ser negativos")
        Integer retrasosPago90d,

        @JsonProperty("tickets_soporte_30d")
        @PositiveOrZero(message = "La cantidad de tickets no puede ser negativa")
        Integer ticketsSoporte30d,

        @JsonProperty("dias_desde_ultimo_login")
        @PositiveOrZero(message = "Los días no pueden ser negativos")
        Integer diasDesdeUltimoLogin,

        @JsonProperty("autopago_activo")
        @NotNull(message = "Debe indicar si el autopago está activo")
        Boolean autopagoActivo
) {}