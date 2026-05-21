package io.github.yienruuuuu.common.controller;

import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.ApiException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(toResponse(exception.getErrorCode().getCode(), exception.getErrorCode().name(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(toResponse(SysCode.INVALID_ARGUMENT.getCode(), SysCode.INVALID_ARGUMENT.name(), message));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequestException(Exception exception) {
        return ResponseEntity.badRequest()
                .body(toResponse(SysCode.INVALID_ARGUMENT.getCode(), SysCode.INVALID_ARGUMENT.name(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Unexpected API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(toResponse(SysCode.INTERNAL_ERROR.getCode(), SysCode.INTERNAL_ERROR.name(), SysCode.INTERNAL_ERROR.getMessage()));
    }

    private ApiErrorResponse toResponse(Integer code, String name, String message) {
        return new ApiErrorResponse(code, name, message, Instant.now());
    }
}
