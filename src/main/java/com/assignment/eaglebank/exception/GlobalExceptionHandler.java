package com.assignment.eaglebank.exception;

import com.assignment.eaglebank.model.BadRequestErrorResponse;
import com.assignment.eaglebank.model.BadRequestErrorResponseDetailsInner;
import com.assignment.eaglebank.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for Eagle Bank API
 * Provides structured error responses with correlation tracking
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles all authentication and authorization related exceptions
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<?> processSecurityViolations(Exception securityException) {
        String correlationId = generateCorrelationId();
        
        if (securityException instanceof BadCredentialsException || 
            (securityException instanceof IllegalArgumentException && 
             securityException.getMessage() != null && 
             securityException.getMessage().contains("Invalid credentials"))) {
            
            log.warn("Authentication failure detected [{}]: {}", correlationId, securityException.getMessage());
            return createUnauthorizedResponse("Invalid credentials provided");
        }
        
        log.warn("Security violation occurred [{}]: {}", correlationId, securityException.getClass().getSimpleName());
        return createUnauthorizedResponse("Authentication required");
    }

    /**
     * Handles access control violations
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> processAccessViolations(AccessDeniedException accessException) {
        String correlationId = generateCorrelationId();
        log.warn("Access denied [{}]: {}", correlationId, accessException.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildSimpleErrorResponse("Insufficient permissions to access this resource"));
    }

    /**
     * Handles business logic violations and conflicts
     */
    @ExceptionHandler({BusinessRuleViolationException.class, InsufficientBalanceException.class})
    public ResponseEntity<ErrorResponse> processBusinessViolations(RuntimeException businessException) {
        String correlationId = generateCorrelationId();
        
        if (businessException instanceof InsufficientBalanceException) {
            log.warn("Insufficient funds detected [{}]: {}", correlationId, businessException.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(buildSimpleErrorResponse(businessException.getMessage()));
        }
        
        log.warn("Business rule violation [{}]: {}", correlationId, businessException.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildSimpleErrorResponse(businessException.getMessage()));
    }

    /**
     * Handles resource not found scenarios
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> processResourceNotFound(ResourceNotFoundException notFoundException) {
        String correlationId = generateCorrelationId();
        log.warn("Resource not found [{}]: {}", correlationId, notFoundException.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildSimpleErrorResponse(notFoundException.getMessage()));
    }

    /**
     * Handles malformed requests and validation errors
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class, 
                      MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<?> processMalformedRequests(Exception malformedRequest) {
        String correlationId = generateCorrelationId();
        
        // Handle JSON parsing errors
        if (malformedRequest instanceof HttpMessageNotReadableException) {
            log.warn("Malformed JSON received [{}]: {}", correlationId, malformedRequest.getMessage());
            return createDetailedBadRequestResponse("Request body contains invalid JSON format", 
                    List.of(createValidationDetail("request", "Invalid JSON format", "json_parse_error")));
        }
        
        // Handle field validation errors
        if (malformedRequest instanceof MethodArgumentNotValidException validationException) {
            log.warn("Field validation failed [{}]: {} field(s) invalid", correlationId, 
                    validationException.getBindingResult().getFieldErrorCount());
            
            List<BadRequestErrorResponseDetailsInner> validationDetails = validationException
                    .getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::mapFieldErrorToDetail)
                    .collect(Collectors.toList());
            
            return createDetailedBadRequestResponse("Request validation failed", validationDetails);
        }
        
        // Handle parameter type mismatches
        if (malformedRequest instanceof MethodArgumentTypeMismatchException typeMismatch) {
            log.warn("Parameter type mismatch [{}]: {}", correlationId, typeMismatch.getName());
            return createDetailedBadRequestResponse("Invalid parameter format", 
                    List.of(createValidationDetail(typeMismatch.getName(), 
                           "Invalid format for parameter: " + typeMismatch.getName(), "type_mismatch")));
        }
        
        // Handle credential validation (special case for IllegalArgumentException)
        if (malformedRequest instanceof IllegalArgumentException illegalArg) {
            if (illegalArg.getMessage() != null && illegalArg.getMessage().contains("Invalid credentials")) {
                return processSecurityViolations(illegalArg);
            }
            
            log.warn("Invalid argument provided [{}]: {}", correlationId, illegalArg.getMessage());
            return createDetailedBadRequestResponse(illegalArg.getMessage(), 
                    List.of(createValidationDetail("request", illegalArg.getMessage(), "validation_error")));
        }
        
        return createDetailedBadRequestResponse("Invalid request format", 
                List.of(createValidationDetail("request", "Request format is invalid", "general_error")));
    }

    /**
     * Handles unexpected system errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> processUnexpectedErrors(Exception systemError) {
        String correlationId = generateCorrelationId();
        log.error("Unexpected system error [{}]: ", correlationId, systemError);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildSimpleErrorResponse("An unexpected error occurred. Please try again later."));
    }

    // Helper methods for response building
    
    private ResponseEntity<ErrorResponse> createUnauthorizedResponse(String message) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildSimpleErrorResponse(message));
    }
    
    private ResponseEntity<BadRequestErrorResponse> createDetailedBadRequestResponse(String message, 
                                                                                   List<BadRequestErrorResponseDetailsInner> details) {
        BadRequestErrorResponse errorResponse = new BadRequestErrorResponse();
        errorResponse.setMessage(message);
        errorResponse.setDetails(details);
        
        return ResponseEntity
                .badRequest()
                .body(errorResponse);
    }
    
    private ErrorResponse buildSimpleErrorResponse(String message) {
        ErrorResponse response = new ErrorResponse();
        response.setMessage(message);
        return response;
    }
    
    private BadRequestErrorResponseDetailsInner createValidationDetail(String field, String message, String type) {
        BadRequestErrorResponseDetailsInner detail = new BadRequestErrorResponseDetailsInner();
        detail.setField(field);
        detail.setMessage(message);
        detail.setType(type);
        return detail;
    }
    
    private BadRequestErrorResponseDetailsInner mapFieldErrorToDetail(FieldError fieldError) {
        return createValidationDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                "validation_error"
        );
    }
    
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
} 