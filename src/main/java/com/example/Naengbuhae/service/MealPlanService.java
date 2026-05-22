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

// 레시피 목록 → Gemini로 끼니 적합성·다양성을 고려한 N일치 식단을 생성.
// 카테고리만으론 "떡볶이를 아침에" 같은 어색한 배치를 막지 못해, 식단 구성만 LLM에 맡긴다.
// 사진 인식 등과 동일한 GEMINI_API_KEY 사용, 식단 페이지당 1회 호출이라 무료 한도 내.
@Slf4j
@Service
public class MealPlanService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MealPlanService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    // 하루 = {breakfast, lunch, dinner} — 값은 입력 레시피 목록의 이름.
    private static final Map<String, Object> DAY_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "breakfast", Map.of("type", "string", "description", "아침 — 레시피 목록의 이름"),
                    "lunch", Map.of("type", "string", "description", "점심 — 레시피 목록의 이름"),
                    "dinner", Map.of("type", "string", "description", "저녁 — 레시피 목록의 이름")
            ),
            "required", List.of("breakfast", "lunch", "dinner")
    );

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("plan", Map.of("type", "array", "items", DAY_SCHEMA)),
            "required", List.of("plan")
    );

    private static final String RULES = """
            규칙:
            - breakfast/lunch/dinner 값은 반드시 아래 '레시피 목록'에 있는 이름을 글자 그대로 사용합니다. 목록에 없는 이름을 지어내지 마세요.
            - 아침은 가벼운 메뉴(계란요리·토스트·요거트·오트밀·스무디·죽·샐러드 등). 떡볶이·튀김·탕수육·찌개·고기볶음 같은 무겁거나 자극적인 메뉴는 아침에 넣지 마세요.
            - 점심·저녁은 제대로 된 한 끼 식사(밥·면·국·찌개·고기·반찬 등).
            - 같은 메뉴 반복을 최소화하고 최대한 다양하게 구성하세요. 같은 날 점심과 저녁은 서로 다르게.
            - 사용자 보유 식재료로 만들 수 있는 메뉴를 가능한 한 우선하되, 끼니 적합성과 다양성을 더 중요하게 고려합니다.
            """;

    // recipes 목록에서 days일치 식단 생성 → [{breakfast, lunch, dinner}, ...].
    // 실패/빈 입력 시 빈 리스트 — 호출 측(프론트)은 기존 규칙 기반 로직으로 fallback한다.
    public List<Map<String, Object>> generate(List<String> recipes, List<String> ingredients, int days)
            throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
        }
        if (recipes == null || recipes.isEmpty()) return List.of();
        int d = Math.min(Math.max(days, 1), 7);

        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 '레시피 목록'만 사용해 ").append(d).append("일치 식단을 만들어주세요.\n\n");
        prompt.append(RULES);
        prompt.append("\n사용자 보유 식재료: ");
        prompt.append(ingredients == null || ingredients.isEmpty()
                ? "(등록된 식재료 없음)" : String.join(", ", ingredients));
        prompt.append("\n\n레시피 목록:\n");
        for (String r : recipes) {
            prompt.append("- ").append(r).append('\n');
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt.toString()))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RESPONSE_SCHEMA,
                        "temperature", 0.7  // 다양성 위해 약간 높게
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
            log.warn("AI 식단 생성: Gemini 응답 텍스트가 없음");
            return List.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(textNode.asText(), HashMap.class);
        Object plan = parsed.get("plan");
        if (!(plan instanceof List<?>)) return List.of();
        return ((List<?>) plan).stream()
                .filter(o -> o instanceof Map)
                .map(o -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) o;
                    return m;
                })
                .toList();
    }
}
