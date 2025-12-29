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
        logger.warn("Petición rechazada por validación. Errores: {}", ex.getBindingResult().getErrorCount());
        var errors = ex.getFieldErrors().stream().map(DataValidationError::new).toList();
        return ResponseEntity.badRequest().body(errors);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Argumento inválido detectado: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }


    @ExceptionHandler({WebClientRequestException.class, java.net.ConnectException.class})
    public ResponseEntity<Map<String, String>> handle503() {
        logger.error("FALLO CRÍTICO: El microservicio de Python (FastAPI) no está disponible.");
        return ResponseEntity.status(503).body(Map.of(
                "error", "Servicio de predicción temporalmente fuera de servicio",
                "mensaje", "Por favor, intente más tarde mientras restablecemos la conexión con la IA."
        ));
    }


    public record DataValidationError(String campo, String mensaje) {
        public DataValidationError(FieldError err) {
            this(err.getField(), err.getDefaultMessage());
        }
    }
}