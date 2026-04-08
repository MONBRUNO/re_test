package com.example.Naengbuhae.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 스프링부트는 정책상 에러가 나도 보여주지 않는데 예외처리를 하므로 인해서 에러를 보여준다
@Hidden // 스웨거 투명 망토 (스웨거가 이 파일을 무시하게 만듦)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 방어막에 걸려서 튕겨났을 때 발생하는 에러만 쏙 낚아챔!
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {

        // DTO에 적어둔 예쁜 에러 메시지("수량은 최소 1개 이상이어야 합니다!") 추출하기
        String errorMessage = ex.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();

        // 까만 창에 400 Bad Request와 함께 메시지를 뱉어줌!
        return ResponseEntity.badRequest().body(errorMessage);
    }
}