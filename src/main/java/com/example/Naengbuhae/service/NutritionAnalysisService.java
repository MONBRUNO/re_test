package com.example.Naengbuhae.service;

import com.example.Naengbuhae.dto.NutritionItemDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

// 외부 AI 서버(capstone-ai, FastAPI) /analyze 엔드포인트 프록시.
// 사진/텍스트를 받아 영양정보 리스트(100g 기준)를 돌려준다.
//
// AI 서버 응답 형태:
//   { "status": "success", "data": [ { "food_name": ..., "cat": ..., "cal": ..., ... }, ... ] }
// 본 서비스는 data 리스트만 추출해서 반환한다.
@Slf4j
@Service
public class NutritionAnalysisService {

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    private final RestTemplate restTemplate;

    public NutritionAnalysisService(RestTemplateBuilder restTemplateBuilder) {
        // AI 서버 응답이 평균 30~60초, 최악 2분까지 걸림 (공공데이터 3페이지 + Gemini 2회 + SDK retry).
        // 발표/시연 시에도 끝까지 응답을 받기 위해 readTimeout 180초로 넉넉히. UI에선 "시간 걸려요" 안내로 커버.
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(180))
                .build();
    }

    public List<NutritionItemDto> analyzeImage(MultipartFile imageFile) throws IOException {
        // ByteArrayResource는 RestTemplate multipart에 파일명/contentType을 같이 실어주기 위한 표준 패턴.
        ByteArrayResource imageResource = new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                String name = imageFile.getOriginalFilename();
                return (name != null && !name.isBlank()) ? name : "image.jpg";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", imageResource);

        return callAnalyze(body);
    }

    public List<NutritionItemDto> analyzeText(String text) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("text", text);

        return callAnalyze(body);
    }

    private List<NutritionItemDto> callAnalyze(MultiValueMap<String, Object> body) {
        String url = aiServerBaseUrl + "/analyze";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("[Nutrition] 📦 AI 서버({}) /analyze 호출", url);
            ResponseEntity<AnalyzeResponse> response =
                    restTemplate.postForEntity(url, request, AnalyzeResponse.class);

            AnalyzeResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getData() == null) {
                log.warn("[Nutrition] AI 서버 응답에 data가 없음");
                return Collections.emptyList();
            }
            log.info("[Nutrition] ✅ {}개 항목 영양정보 받음", responseBody.getData().size());
            return responseBody.getData();

        } catch (RestClientException e) {
            log.error("[Nutrition] ❌ AI 서버({}) 통신 실패: {}", url, e.getMessage());
            throw new RuntimeException("AI 영양분석 서버와의 통신이 원활하지 않습니다.");
        }
    }

    // AI 서버 응답 wrapper — 내부 매핑 전용.
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AnalyzeResponse {
        private String status;
        private List<NutritionItemDto> data;
    }
}
