package com.example.Naengbuhae.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 사진을 받아서 Gemini Vision API에 전달 → 식재료 정보를 JSON으로 받아오는 서비스.
// 사진 자체는 저장하지 않음. API 호출 후 메모리에서 폐기.
@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientRecognitionService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    // 응답 JSON 구조 정의 (Gemini의 structured output 기능 사용 — 모델이 이 스키마대로 답하도록 강제).
    // expiryDaysByStorage: 보관 방법별 권장 일수. 사용자가 보관 드롭다운을 바꿔도
    // 그에 맞는 일수로 유통기한이 재계산되도록 셋 다 받음.
    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "name", Map.of("type", "string", "description", "식재료 이름 (한국어, 짧고 일반적)"),
                    "category", Map.of("type", "string", "enum",
                            List.of("채소", "육류", "유제품", "곡물", "해산물", "과일",
                                    "가공식품", "음료", "조미료", "간식", "기타")),
                    "storage", Map.of("type", "string", "enum",
                            List.of("냉장", "냉동", "실온")),
                    "quantity", Map.of("type", "number", "description", "사진에 보이는 수량"),
                    "unit", Map.of("type", "string", "enum",
                            List.of("개", "g", "kg", "ml", "L", "팩")),
                    "expiryDaysByStorage", Map.of(
                            "type", "object",
                            "description", "보관 방법별 권장 유통기한 일수. 부적절한 방법은 0 또는 작은 값",
                            "properties", Map.of(
                                    "냉장", Map.of("type", "integer"),
                                    "냉동", Map.of("type", "integer"),
                                    "실온", Map.of("type", "integer")
                            ),
                            "required", List.of("냉장", "냉동", "실온")
                    )
            ),
            "required", List.of("name", "category", "storage", "quantity", "unit", "expiryDaysByStorage")
    );

    private static final String PROMPT = """
            당신은 식재료 인식 전문가입니다. 이 사진 속 식재료의 정보를 알려주세요.

            규칙:
            - 여러 식재료가 있으면 가장 두드러진 하나만 선택
            - 한국어로 짧고 일반적인 이름을 사용 (예: 양배추, 햄, 우유)
            - storage는 일반적으로 권장되는 보관 방법 (예: 햄·우유는 냉장, 만두·아이스크림은 냉동, 감자·바나나는 실온)
            - quantity는 사진에서 보이는 개수 또는 추정 수량
            - unit은 가장 자연스러운 단위 (개수면 '개', 무게면 'g'/'kg', 액체면 'ml'/'L', 포장식품이면 '팩')

            category 분류 가이드 (정확하게 골라주세요):
            - 채소: 생채소 (양배추, 양상추, 당근, 감자, 마늘, 대파...)
            - 육류: 생/가공 고기 (소고기, 돼지고기, 닭고기, 햄, 베이컨, 소시지, 스팸...)
            - 유제품: 우유, 요거트, 치즈, 버터, 생크림...
            - 곡물: 생 곡류/가루만 (쌀, 잡곡, 밀가루, 식빵 등 기본 빵류 포함). ⚠ 라면·즉석면은 곡물 아님!
            - 해산물: 생선, 새우, 오징어, 조개...
            - 과일: 사과, 바나나, 딸기, 포도...
            - 가공식품: 라면·즉석면(짜파게티·신라면 등), 즉석밥, 통조림, 냉동만두/피자/돈까스, HMR, 햇반...
            - 음료: 음료수, 주스, 탄산음료, 물, 차 (단 우유는 유제품)
            - 조미료: 간장·고추장·된장 같은 장류, 소금·후추·설탕, 식초·참기름, 소스, 케첩, 마요네즈
            - 간식: 과자, 초콜릿, 사탕, 케이크, 아이스크림, 빵류 중 디저트 (단팥빵·크림빵), 견과류 스낵
            - 기타: 위 분류에 명확히 안 들어가는 것 (계란, 두부, 묵, 김 등 발효/단백 식품)

            expiryDaysByStorage: 각 보관 방법으로 보관할 때의 구매일 기준 일반적인 유통기한(일수).
            한국 마트에서 일반적으로 유통되는 제품의 제조사 표시 유통기한 기준으로 답해주세요.
            부적절한 보관(예: 생고기·우유 실온, 아이스크림 실온)은 0 또는 1-2일.
            대략적 감각:
            - 신선식품(생채소·생고기·생선·우유): 냉장 며칠~1-2주
            - 잎채소 vs 뿌리채소: 잎채소 짧음(5-7일), 뿌리채소 김(2-4주)
            - 냉동 가공식품(만두·피자·돈까스·HMR): 냉동 6-12개월 (제조사 표기 기준)
            - 라면·통조림·즉석밥: 실온 6개월~1년 이상
            - 조미료·향신료: 미개봉 1-2년, 개봉 후 6-12개월
            - 과일: 종류에 따라 며칠~2주
            """;

    public Map<String, Object> recognize(MultipartFile imageFile) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
        String mimeType = imageFile.getContentType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "image/jpeg";
        }

        // Gemini 요청 본문 구성
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", PROMPT),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Image
                                ))
                        )
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

        // 응답에서 텍스트 추출 → JSON 파싱
        // 구조: { candidates: [ { content: { parts: [ { text: "..." } ] } } ] }
        if (response == null) return null;
        JsonNode root = objectMapper.valueToTree(response);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            log.warn("Gemini 응답에 candidates가 없음: {}", response);
            return null;
        }
        JsonNode textNode = candidates.get(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            log.warn("Gemini 응답에서 인식 결과 텍스트를 찾을 수 없음");
            return null;
        }

        String resultJson = textNode.asText();
        @SuppressWarnings("unchecked")
        Map<String, Object> recognized = objectMapper.readValue(resultJson, HashMap.class);
        return recognized;
    }

    // === 영수증 인식: 영수증 사진에서 여러 식재료 추출 ===

    // 영수증 응답 스키마: 단품 스키마(name/category/storage/...)를 items 배열로 감쌈.
    private static final Map<String, Object> RECEIPT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "items", Map.of(
                            "type", "array",
                            "items", RESPONSE_SCHEMA
                    )
            ),
            "required", List.of("items")
    );

    private static final String RECEIPT_PROMPT = """
            마트/슈퍼 영수증 사진입니다. 영수증에 적힌 모든 식재료 품목을 찾아 알려주세요.

            규칙:
            - 식재료/식품만 포함하세요. 비식품(쓰레기봉투, 세제, 영수증 합계, 결제정보 등)은 제외.
            - 각 품목에 대해 단품 인식과 동일한 정보를 채워주세요 (name, category, storage, quantity, unit, expiryDaysByStorage).
            - 영수증의 상품명이 줄임말이나 코드 같으면 추정해서 일반적인 한국어 식재료 이름으로 변환 (예: "냉동만두진" → "냉동만두", "올리브짜파" → "올리브 짜파게티").
            - quantity는 영수증의 수량 정보를 사용하되, 없으면 1로.
            - storage는 일반적 권장 보관법 (소고기·우유 냉장, 만두·아이스크림 냉동, 라면·통조림 실온 등).
            - expiryDaysByStorage는 보관별 권장 일수 (단품 인식과 동일한 가이드).
            - 식별이 어려운 항목은 건너뛰세요.

            category 분류 가이드 (반드시 정확하게):
            - 채소: 생채소 (양배추, 양상추, 당근, 감자, 마늘, 대파...)
            - 육류: 생/가공 고기 (소고기, 돼지고기, 닭고기, 햄, 베이컨, 소시지, 스팸...)
            - 유제품: 우유, 요거트, 치즈, 버터, 생크림...
            - 곡물: 생 곡류/가루만 (쌀, 잡곡, 밀가루, 식빵 등 기본 빵류). ⚠ 라면·짜파게티·즉석면은 곡물 아님!
            - 해산물: 생선, 새우, 오징어, 조개...
            - 과일: 사과, 바나나, 딸기, 포도...
            - 가공식품: 라면·짜파게티·신라면·즉석면, 즉석밥, 통조림, 냉동만두/피자/돈까스, HMR, 햇반...
            - 음료: 음료수, 주스, 탄산음료, 물, 차 (단 우유는 유제품)
            - 조미료: 간장·고추장·된장 같은 장류, 소금·후추·설탕, 식초·참기름, 소스, 케첩, 마요네즈
            - 간식: 과자, 초콜릿, 사탕, 케이크, 아이스크림, 단팥빵·크림빵 등 디저트 빵, 견과류 스낵
            - 기타: 위 분류에 명확히 안 들어가는 것 (계란, 두부, 묵, 김 등)
            """;

    public List<Map<String, Object>> recognizeReceipt(MultipartFile imageFile) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
        String mimeType = imageFile.getContentType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "image/jpeg";
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", RECEIPT_PROMPT),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Image
                                ))
                        )
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RECEIPT_SCHEMA,
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
            log.warn("영수증 인식: 응답 텍스트가 없음");
            return List.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(textNode.asText(), HashMap.class);
        Object items = parsed.get("items");
        if (!(items instanceof List<?>)) return List.of();
        return ((List<?>) items).stream()
                .filter(o -> o instanceof Map)
                .map(o -> (Map<String, Object>) o)
                .toList();
    }
}
