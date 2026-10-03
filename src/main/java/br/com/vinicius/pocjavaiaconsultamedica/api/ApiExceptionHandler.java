package br.com.vinicius.pocjavaiaconsultamedica.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte exceções inesperadas em JSON legível.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Requisição inválida.");

        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_error",
                "message", message
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpectedError(Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "internal_error");
        body.put("message", exception.getMessage() == null
                ? "Ocorreu um erro inesperado."
                : exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
