package anam_145.SpringBoot.Server.apiPayload.exception;

import anam_145.SpringBoot.Server.apiPayload.ApiResponse;
import anam_145.SpringBoot.Server.apiPayload.code.ErrorReasonDTO;
import anam_145.SpringBoot.Server.apiPayload.code.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    // Validation 예외 처리 (@Valid 검증 실패)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(fieldName, errorMessage, (existingErrorMessage, newErrorMessage) ->
                    existingErrorMessage + ", " + newErrorMessage);
        });

        ErrorReasonDTO errorReasonHttpStatus = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();

        return handleExceptionInternalArgs(
                ex,
                HttpStatus.valueOf(errorReasonHttpStatus.getHttpStatus().value()),
                headers,
                errorReasonHttpStatus,
                request,
                errors
        );
    }

    // ConstraintViolation 예외 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ConstraintViolationException 추출 도중 에러 발생"));

        ErrorReasonDTO errorReasonHttpStatus = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();

        return handleExceptionInternalConstraint(
                ex,
                errorReasonHttpStatus,
                HttpHeaders.EMPTY,
                request,
                errorMessage
        );
    }

    // 커스텀 GeneralException 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> handleGeneralException(
            GeneralException ex,
            WebRequest request) {

        ErrorReasonDTO errorReasonHttpStatus = ex.getErrorReasonHttpStatus();

        return handleExceptionInternalFalse(
                ex,
                errorReasonHttpStatus,
                HttpHeaders.EMPTY,
                errorReasonHttpStatus.getHttpStatus(),
                request,
                ex.getMessage()
        );
    }

    // 기타 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorReasonDTO errorReasonHttpStatus = ErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus();

        return handleExceptionInternalFalse(
                ex,
                errorReasonHttpStatus,
                HttpHeaders.EMPTY,
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                ex.getMessage()
        );
    }

    // 내부 헬퍼 메서드 - Validation 에러용
    private ResponseEntity<Object> handleExceptionInternalArgs(
            Exception ex,
            HttpStatus status,
            HttpHeaders headers,
            ErrorReasonDTO errorReasonHttpStatus,
            WebRequest request,
            Map<String, String> errorArgs) {

        ApiResponse<Object> body = ApiResponse.onFailure(
                errorReasonHttpStatus.getCode(),
                errorReasonHttpStatus.getMessage(),
                errorArgs
        );

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        log.error("[Validation Error] {} : {}", servletWebRequest.getRequest().getRequestURL(), errorArgs);

        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    // 내부 헬퍼 메서드 - Constraint 에러용
    private ResponseEntity<Object> handleExceptionInternalConstraint(
            Exception ex,
            ErrorReasonDTO errorReasonHttpStatus,
            HttpHeaders headers,
            WebRequest request,
            String errorPoint) {

        ApiResponse<Object> body = ApiResponse.onFailure(
                errorReasonHttpStatus.getCode(),
                errorReasonHttpStatus.getMessage(),
                errorPoint
        );

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        log.error("[Constraint Error] {} : {}", servletWebRequest.getRequest().getRequestURL(), errorPoint);

        return super.handleExceptionInternal(ex, body, headers, errorReasonHttpStatus.getHttpStatus(), request);
    }

    // 내부 헬퍼 메서드 - 일반 에러용
    private ResponseEntity<Object> handleExceptionInternalFalse(
            Exception ex,
            ErrorReasonDTO errorReasonHttpStatus,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request,
            String errorPoint) {

        ApiResponse<Object> body = ApiResponse.onFailure(
                errorReasonHttpStatus.getCode(),
                errorReasonHttpStatus.getMessage(),
                errorPoint
        );

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        log.error("[Exception] {} : {}", servletWebRequest.getRequest().getRequestURL(), errorPoint);

        return super.handleExceptionInternal(ex, body, headers, status, request);
    }
}
