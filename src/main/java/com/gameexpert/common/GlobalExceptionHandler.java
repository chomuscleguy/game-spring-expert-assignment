package com.gameexpert.common;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getError());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        return build(HttpStatus.FORBIDDEN, e.getError());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, e.getError());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getError());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e.getError());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation() {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch() {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable() {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoHandler() {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(HttpServletRequest request) {
        log.debug("클라이언트 커넥션 중단: {} {}", request.getMethod(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        if (isClientAbort(e)) {
            log.debug("클라이언트 커넥션 중단(감싸인 예외): {} {}", request.getMethod(), request.getRequestURI());
            return ResponseEntity.<ErrorResponse>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.error("예상치 못한 오류: {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }

    private static boolean isClientAbort(Throwable t) {
        return Stream.iterate(t, Objects::nonNull,
                        cause -> cause.getCause() == cause ? null : cause.getCause())
                .anyMatch(cause -> {
                    if (cause instanceof ClientAbortException) {
                        return true;
                    }
                    if (cause instanceof IOException && cause.getMessage() != null) {
                        String message = cause.getMessage().toLowerCase();
                        return message.contains("broken pipe") || message.contains("connection reset");
                    }
                    return false;
                });
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error) {
        return ResponseEntity.status(status).body(new ErrorResponse(error));
    }
}
