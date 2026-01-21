package com.alura.churnnsight.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Error 400 - Bean Validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handlerError400(MethodArgumentNotValidException ex){
        var errors = ex.getFieldErrors().stream()
                .map(DataValidationError::new)
                .toList();

        return ResponseEntity.badRequest().body(
                Map.of(
                        "message", "Hay campos inválidos en la solicitud.",
                        "code", "VALIDATION_ERROR",
                        "details", errors
                )
        );
    }

    // Error 400 - JSON mal formado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                Map.of( "message", "El JSON de la solicitud es inválido o no se puede leer.",
                        "code", "MALFORMED_JSON",
                        "details", List.of())
                );
    }

    // Error 400 - argumentos inválidos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "message", ex.getMessage(),
                        "code", "BAD_REQUEST",
                        "details", List.of()
                )
        );
    }

    // Error 404 - recurso no encontrado en BD
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(
                        "message", ex.getMessage(),
                        "code", "NOT_FOUND",
                        "details", List.of()
                )
        );
    }

    // Error 422 - reglas de negocio
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                Map.of(
                        "message", ex.getMessage(),
                        "code", ex.getCode(),
                        "details", List.of()
                )
        );
    }

    // Error 409 - CreationException
    @ExceptionHandler(CreationException.class)
    public ResponseEntity<Map<String, Object>> handleCreation(CreationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(
                        "message", ex.getMessage(),
                        "code", "CREATION_ERROR",
                        "details", List.of()
                )
        );
    }

    // Error 502 o 504- FastAPI/downstream
    @ExceptionHandler(DownstreamException.class)
    public ResponseEntity<Map<String, Object>> handleDownstream(DownstreamException ex) {
        int status = ex.getStatus(); // 502, 504
        String code = (status == 504) ? "DOWNSTREAM_TIMEOUT" : "DOWNSTREAM_ERROR";
        return ResponseEntity.status(status).body(
                Map.of(
                        "message", "Error comunicándose con el servicio de predicción.",
                        "code", code,
                        "details", List.of(
                                Map.of(
                                        "service", ex.getService(),
                                        "status", ex.getStatus()
                                )
                        )
                )
        );
    }

    // Error 500 - fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "message", "Ocurrió un error inesperado.",
                        "code", "INTERNAL_ERROR",
                        "details", List.of()
                )
        );
    }

    public record DataValidationError(String field, String message){
        public DataValidationError(FieldError err){
            this(err.getField(), err.getDefaultMessage());
        }
    }
}
