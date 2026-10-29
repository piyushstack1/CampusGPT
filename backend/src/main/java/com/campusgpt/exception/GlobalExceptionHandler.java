package com.campusgpt.exception;

import com.campusgpt.dto.Dto.ErrorResponse;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ErrorResponse> api(ApiException ex) {
    return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse("REQUEST_FAILED", ex.getMessage(), Instant.now()));
  }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", "Request validation failed", Instant.now()));
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> generic(Exception ex) {
    log.error("Unhandled API error", ex);
    return ResponseEntity.status(500).body(new ErrorResponse("INTERNAL_ERROR", "Unexpected server error", Instant.now()));
  }
}
