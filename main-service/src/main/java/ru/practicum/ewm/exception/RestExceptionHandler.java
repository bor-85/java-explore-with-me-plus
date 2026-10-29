package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.dto.api.ApiError;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final String REASON_NOT_FOUND = "The required object was not found.";
    private static final String REASON_CONFLICT = "For the requested operation the conditions are not met.";
    private static final String REASON_BAD_REQUEST = "Incorrectly made request.";
    private static final String REASON_INTEGRITY = "Integrity constraint has been violated.";
    private static final String REASON_INTERNAL = "Internal server error.";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return buildError(
                HttpStatus.NOT_FOUND,
                REASON_NOT_FOUND,
                ex.getMessage(),
                List.of(ex.getMessage())
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return buildError(
                HttpStatus.CONFLICT,
                REASON_CONFLICT,
                ex.getMessage(),
                List.of(ex.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        String message = errors.isEmpty() ? ex.getMessage() : errors.get(0);

        return buildError(
                HttpStatus.BAD_REQUEST,
                REASON_BAD_REQUEST,
                message,
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(this::formatViolation)
                .toList();

        String message = errors.isEmpty() ? ex.getMessage() : errors.get(0);

        return buildError(
                HttpStatus.BAD_REQUEST,
                REASON_BAD_REQUEST,
                message,
                errors
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class,
            ValidationException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                REASON_BAD_REQUEST,
                ex.getMessage(),
                List.of(ex.getMessage())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        return buildError(
                HttpStatus.CONFLICT,
                REASON_INTEGRITY,
                message,
                List.of(message)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                REASON_INTERNAL,
                ex.getMessage(),
                List.of(ex.getMessage())
        );
    }

    private String formatFieldError(FieldError error) {
        return "Field: " + error.getField()
                + ". Error: " + error.getDefaultMessage()
                + ". Value: " + error.getRejectedValue();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        return "Field: " + violation.getPropertyPath()
                + ". Error: " + violation.getMessage()
                + ". Value: " + violation.getInvalidValue();
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status,
                                                String reason,
                                                String message,
                                                List<String> errors) {
        ApiError apiError = new ApiError(
                errors,
                message,
                reason,
                status.name(),
                LocalDateTime.now().format(FORMATTER)
        );
        return ResponseEntity.status(status).body(apiError);
    }
}