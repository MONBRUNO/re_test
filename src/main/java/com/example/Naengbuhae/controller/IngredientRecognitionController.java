package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.service.IngredientRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// POST /api/ingredients/recognize
// 카메라/갤러리에서 받은 사진을 Gemini Vision API에 전달 → 식재료 정보 반환.
// 사진 자체는 저장하지 않음 (인식만 하고 폐기).
@Slf4j
@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientRecognitionController {

    private final IngredientRecognitionService recognitionService;

    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognize(
            @RequestParam("image") MultipartFile image,
            Principal principal
    ) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일이 비어있습니다."));
        }

        try {
            Map<String, Object> recognized = recognitionService.recognize(image);
            Map<String, Object> body = new HashMap<>();
            body.put("recognized", recognized); // null이어도 그대로 반환 (앱이 빈 폼으로 처리)
            return ResponseEntity.ok(body);
        } catch (IllegalStateException e) {
            // API 키 미설정 등 설정 문제
            log.error("Gemini 설정 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "AI 인식 기능을 사용할 수 없습니다."));
        } catch (Exception e) {
            log.error("식재료 인식 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "인식 중 오류가 발생했습니다."));
        }
    }

    // 영수증 사진에서 여러 식재료 일괄 추출. 사용자가 화면에서 확인/수정 후 저장.
    @PostMapping("/recognize-receipt")
    public ResponseEntity<Map<String, Object>> recognizeReceipt(
            @RequestParam("image") MultipartFile image,
            Principal principal
    ) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일이 비어있습니다."));
        }
        try {
            List<Map<String, Object>> items = recognitionService.recognizeReceipt(image);
            Map<String, Object> body = new HashMap<>();
            body.put("items", items);
            return ResponseEntity.ok(body);
        } catch (IllegalStateException e) {
            log.error("Gemini 설정 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "AI 인식 기능을 사용할 수 없습니다."));
        } catch (Exception e) {
            log.error("영수증 인식 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "인식 중 오류가 발생했습니다."));
        }
    }
}
