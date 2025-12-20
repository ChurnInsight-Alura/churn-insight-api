package com.alura.churnnsight.model.enumeration;

public enum Prevision {
    CANCELA,
    PERMANECE;

    public static Prevision fromPrediction(Double prob) {
        return prob >= 0.5 ? CANCELA : PERMANECE;
    }
}
