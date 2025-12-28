package com.alura.churnnsight.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;


public class PredictRequestDTO {

    @NotNull(message = "tiempo_contrato_meses es obligatorio")
    @Min(value = 1, message = "tiempo_contrato_meses debe ser mayor a 0")
    private Integer tiempoContratoMeses;

    @NotNull(message = "retrasos_pago es obligatorio")
    @Min(value = 0, message = "retrasos_pago no puede ser negativo")
    private Integer retrasosPago;

    @NotNull(message = "uso_mensual es obligatorio")
    @DecimalMin(value = "0.0", message = "uso_mensual debe ser >= 0")
    private Double usoMensual;

    @NotBlank(message = "plan es obligatorio")
    private String plan;

    public Integer getTiempoContratoMeses() {
        return tiempoContratoMeses;
    }

    public void setTiempoContratoMeses(Integer tiempoContratoMeses) {
        this.tiempoContratoMeses = tiempoContratoMeses;
    }

    public Integer getRetrasosPago() {
        return retrasosPago;
    }

    public void setRetrasosPago(Integer retrasosPago) {
        this.retrasosPago = retrasosPago;
    }

    public Double getUsoMensual() {
        return usoMensual;
    }

    public void setUsoMensual(Double usoMensual) {
        this.usoMensual = usoMensual;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    // getters y setters

}
