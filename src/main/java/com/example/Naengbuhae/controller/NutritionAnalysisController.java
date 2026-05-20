package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.NutritionItemDto;
import com.example.Naengbuhae.service.NutritionAnalysisService;
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
import java.util.List;
import java.util.Map;

// 영양분석 — 외부 AI 서버(capstone-ai) /analyze 프록시.
// 식재료 등록용 인식(/api/ingredients/recognize)과는 별개 기능. 출력이 영양정보(kcal/단백질/탄수/지방)임.
// 입력은 사진(file) 또는 텍스트(text) 둘 중 하나.
@Slf4j
@RestController
@RequestMapping("/api/nutrition")
@RequiredArgsConstructor
public class NutritionAnalysisController {

    private final NutritionAnalysisService nutritionAnalysisService;

    // POST /api/nutrition/analyze
    // multipart/form-data:
    //   - file (이미지)  → 사진 기반 분석
    //   - text (문자열) → 텍스트 기반 분석
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "text", required = false) String text,
            Principal principal
    ) {
        if ((file == null || file.isEmpty()) && (text == null || text.isBlank())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "file 또는 text 중 하나는 필요합니다."));
        }

        try {
            List<NutritionItemDto> items = (file != null && !file.isEmpty())
                    ? nutritionAnalysisService.analyzeImage(file)
                    : nutritionAnalysisService.analyzeText(text);
            return ResponseEntity.ok(Map.of("data", items));
        } catch (RuntimeException e) {
            // AI 서버 통신 실패 (서버 다운/타임아웃 등) — 503.
            log.error("영양분석 실패", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("영양분석 처리 중 예외", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "영양분석 처리 중 오류가 발생했습니다."));
        }
    }
}
