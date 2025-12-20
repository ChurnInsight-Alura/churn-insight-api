package com.alura.churnnsight.dto;

import com.alura.churnnsight.model.enumeration.Prevision;

public record DataPredictionResult(
        Prevision prevision,
        Double probabilidad
) {

}
