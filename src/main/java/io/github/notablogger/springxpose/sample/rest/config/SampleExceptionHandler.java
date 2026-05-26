package io.github.notablogger.springxpose.sample.rest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local exception handler for the sample application.
 * Overrides the library's handler to provide sanitized constraint violation responses.
 */
@RestControllerAdvice
public class SampleExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SampleExceptionHandler.class);

    /**
     * Handle database constraint violations without exposing SQL details.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {
        log.warn("Data integrity violation while processing request", ex);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "urn:springxpose:constraint-violation");
        response.put("title", "Data integrity violation");
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("detail", "The request conflicts with existing data. Verify referenced IDs and unique values.");
        response.put("instance", request.getDescription(false).replace("uri=", ""));
        response.put("errorCode", "CONSTRAINT_VIOLATION");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}

