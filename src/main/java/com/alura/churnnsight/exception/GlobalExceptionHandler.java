package com.alura.churnnsight.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<DataValidationError>> handle400(MethodArgumentNotValidException ex) {
        // Log seguro: No muestra datos del cliente
        logger.warn("Petición rechazada: {} error(es) de validación encontrados.", ex.getBindingResult().getErrorCount());
        var errors = ex.getFieldErrors().stream().map(DataValidationError::new).toList();
        return ResponseEntity.badRequest().body(errors);
    }


    @ExceptionHandler({WebClientRequestException.class, java.net.ConnectException.class})
    public ResponseEntity<Map<String, String>> handle503() {
        logger.error("FALLO CRÍTICO: El microservicio FastAPI no está disponible.");
        return ResponseEntity.status(503).body(Map.of(
                "error", "Servicio de IA fuera de servicio",
                "mensaje", "No se pudo conectar con el modelo de predicción. Intente más tarde."
        ));
    }

    public record DataValidationError(String campo, String mensaje) {
        public DataValidationError(FieldError err) {
            this(err.getField(), err.getDefaultMessage());
        }
    }
}