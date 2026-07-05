package it.marconi.biblioteca.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import it.marconi.biblioteca.domain.APIResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 - validazione fallita su @Valid nei DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(APIResponse.fail(errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleResponseStatus(ResponseStatusException ex) {

        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).body(APIResponse.fail(errors));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleGeneric(Exception ex) {

        return ResponseEntity.internalServerError()
                .body(APIResponse.error("Si è verificato un errore interno al server"));
    }
}