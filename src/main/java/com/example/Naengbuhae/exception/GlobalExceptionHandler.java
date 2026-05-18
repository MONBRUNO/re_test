package com.example.Naengbuhae.exception;

import com.example.Naengbuhae.user.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid @RequestBody 검증 실패. 모든 field 에러를 "; "로 합쳐서 한 번에 노출.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "잘못된 입력")
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, errorMessage));
    }

    // @Validated가 붙은 컨트롤러의 path/query 파라미터 검증 실패.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, errorMessage));
    }

    // 필수 query/form 파라미터 누락.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "필수 파라미터가 누락되었습니다: " + ex.getParameterName()));
    }

    // path variable / query param 타입 불일치 (예: UUID 자리에 '1'이 들어오는 경우 등)
    // ✨ 보안 패치: 기술 스택 노출 방지 및 사용자 친화적 에러 메시지 반환
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("[Security Warning] 잘못된 타입 요청 차단: 파라미터명={}, 입력값={}", ex.getName(), ex.getValue());
        
        String message = "잘못된 요청 형식입니다. (ID 형식이 올바르지 않습니다)";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(false, "데이터 중복 또는 제약조건 위반이 발생했습니다."));
    }

    // 서비스 계층에서 검증 실패로 던지는 예외 (사용자 없음, 잘못된 토큰 등).
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, ex.getMessage()));
    }

    // 날짜 슬래시(/) 입력, 숫자 칸에 문자, enum에 정의되지 않은 값 등 JSON 파싱 단계 에러.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, "입력값이 잘못되었습니다. 날짜는 'yyyy-MM-dd' 형식(슬래시 불가)이어야 하며, 데이터 타입이 맞는지 확인해주세요."));
    }

    // @PreAuthorize 등 권한 체크 실패. 인증은 되어있지만 권한이 부족한 경우.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "이 작업을 수행할 권한이 없습니다."));
    }

    // catch-all: 위에서 잡지 못한 예기치 못한 모든 예외.
    // 사용자에겐 일반적인 메시지를 주고, 서버 로그에는 스택트레이스를 남겨 디버깅 가능하게 함.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception ex) {
        log.error("[GlobalExceptionHandler] 처리되지 않은 예외", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }
}
