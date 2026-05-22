package com.example.Naengbuhae.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 식재료 이름 목록 → Gemini로 100g당 영양정보(칼로리·단백질·탄수·지방) 일괄 조회.
// 영양분석 페이지가 열릴 때 호출한다. 사진 인식(IngredientRecognitionService)과 동일한
// Gemini 키/모델을 쓰며, 호출은 페이지 1회당 1번이라 무료 한도 안에서 동작한다.
@Slf4j
@Service
public class IngredientNutritionService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngredientNutritionService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    // 항목 스키마 — 100g(액체는 100ml) 기준 영양정보
    private static final Map<String, Object> ITEM_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "name", Map.of("type", "string", "description", "입력받은 식재료 이름 그대로"),
                    "calories", Map.of("type", "number", "description", "100g당 칼로리 kcal"),
                    "protein", Map.of("type", "number", "description", "100g당 단백질 g"),
                    "carbs", Map.of("type", "number", "description", "100g당 탄수화물 g"),
                    "fat", Map.of("type", "number", "description", "100g당 지방 g")
            ),
            "required", List.of("name", "calories", "protein", "carbs", "fat")
    );

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("items", Map.of("type", "array", "items", ITEM_SCHEMA)),
            "required", List.of("items")
    );

    private static final String PROMPT_HEADER = """
            아래 식재료/식품 각각의 100g(액체는 100ml)당 영양정보를 알려주세요.
            - calories(kcal), protein(g), carbs(g), fat(g) 네 가지를 채웁니다.
            - 가공식품·브랜드 제품(예: '농심 튀김우동 큰사발')은 일반적인 시판 제품 기준으로 추정합니다.
            - name은 입력받은 이름을 그대로 반환합니다.
            - 정확히 모르는 항목도 같은 종류 식품 기준으로 합리적으로 추정해 채웁니다.

            식재료 목록:
            """;

    // 이름 목록 → [{name, calories, protein, carbs, fat}, ...] (100g 기준).
    // 조회 실패/빈 입력 시 빈 리스트 — 호출 측(웹)은 내장 DB로 fallback한다.
    public List<Map<String, Object>> lookup(List<String> names) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
        }
        if (names == null || names.isEmpty()) return List.of();

        StringBuilder prompt = new StringBuilder(PROMPT_HEADER);
        for (String n : names) {
            prompt.append("- ").append(n).append('\n');
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt.toString()))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RESPONSE_SCHEMA,
                        "temperature", 0.2
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = GEMINI_URL + "?key=" + apiKey;

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response == null) return List.of();
        JsonNode root = objectMapper.valueToTree(response);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            log.warn("식재료 영양정보 조회: Gemini 응답 텍스트가 없음");
            return List.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(textNode.asText(), HashMap.class);
        Object items = parsed.get("items");
        if (!(items instanceof List<?>)) return List.of();
        return ((List<?>) items).stream()
                .filter(o -> o instanceof Map)
                .map(o -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) o;
                    return m;
                })
                .toList();
    }
}
