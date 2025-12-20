package com.alura.churnnsight.dto;

import com.alura.churnnsight.model.enumeration.Plan;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import jakarta.validation.constraints.*;

public record DataMakePrediction(

        @NotNull(message = "El id no puede ser nulo")
        Long id,

        @JsonProperty("tiempo_contrato_meses")
        @PositiveOrZero(message = "El tiempo de contrato debe ser 0 o mayor")
        Integer tiempoContratoMeses,

        @NotNull(message = "El plan es obligatorio")
        Plan plan,

        @JsonProperty("uso_mensual")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "El uso mensual debe ser mayor o igual a 0")
        Float usoMensual,

        @JsonProperty("retrasos_pago_90d")
        @PositiveOrZero(message = "Los retrasos de pago no pueden ser negativos")
        Integer retrasosPago90d,

        @JsonProperty("tickets_soporte_30d")
        @PositiveOrZero(message = "Los tickets de soporte no pueden ser negativos")
        Integer ticketsSoporte30d,

        @JsonProperty("dias_desde_ultimo_login")
        @PositiveOrZero(message = "Los dias desde el ultimo login no pueden ser negativos")
        Integer diasDesdeUltimoLogin,

        @JsonProperty("autopago_activo")
        @NotNull(message = "Debe indicarse si el autopago está activo")
        Boolean autopagoActivo
) {}

