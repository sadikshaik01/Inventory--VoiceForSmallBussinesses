package com.voicestock.exception;

import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException error) { return ResponseEntity.status(error.getStatus()).body(Map.of("message",error.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException error) {
        var field = error.getBindingResult().getFieldErrors().getFirst();
        return ResponseEntity.badRequest().body(Map.of("message",field.getField()+": "+field.getDefaultMessage()));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception error) { return ResponseEntity.badRequest().body(Map.of("message","Check the entered values and try again.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(Exception error) { return ResponseEntity.status(409).body(Map.of("message","This record conflicts with existing data. Check your values.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception error) {
        LoggerFactory.getLogger(getClass()).error("Request failed: {}", error.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(Map.of("message","Something went wrong. Please try again."));
    }
}
