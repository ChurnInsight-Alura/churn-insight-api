package com.alura.churnnsight.exception;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Error 400 - Bean Validation (DTOs con @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handlerError400(MethodArgumentNotValidException ex){
        var errors = ex.getFieldErrors().stream().map(DataValidationError::new).toList();

        // "message" siempre presente (compatibilidad)
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
                        "code", "MALFORMED_JSON" )
                );
    }

    // Error 400 - argumentos inválidos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity.badRequest().body(
                Map.of("error", ex.getMessage(),
                        "message", ex.getMessage()
                )
        );
    }

    // Error 404 - si creas NotFoundException
    @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ChangeSetPersister.NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of(
                        "message", ex.getMessage(),
                        "code", "NOT_FOUND"
                )
        );
    }

    // Error 422 - reglas de negocio (si creas BusinessException)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                Map.of(
                        "message", ex.getMessage(),
                        "code", ex.getCode()
                )
        );
    }

    // Error 409 - CreationException
    @ExceptionHandler(CreationException.class)
    public ResponseEntity<Map<String, String>> handleCreation(CreationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(
                        "message", ex.getMessage(),
                        "code", "CREATION_ERROR"
                )
        );
    }

    // Error 502 - FastAPI/downstream (si creas DownstreamException)
    @ExceptionHandler(DownstreamException.class)
    public ResponseEntity<Map<String, String>> handleDownstream(DownstreamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                Map.of(
                        "message", "Error comunicándose con el servicio de predicción.",
                        "code", "DOWNSTREAM_ERROR"
                )
        );
    }

    // Error 500 - fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "message", "Ocurrió un error inesperado.",
                        "code", "INTERNAL_ERROR"
                )
        );
    }

    public record DataValidationError(String field, String message){
        public DataValidationError(FieldError err){
            this(err.getField(), err.getDefaultMessage());
        }
    }

}
