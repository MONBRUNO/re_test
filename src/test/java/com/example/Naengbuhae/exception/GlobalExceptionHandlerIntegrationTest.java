package com.example.Naengbuhae.exception;

import com.example.Naengbuhae.config.CustomOAuth2UserService;
import com.example.Naengbuhae.config.JwtAuthenticationFilter;
import com.example.Naengbuhae.config.JwtUtil;
import com.example.Naengbuhae.config.OAuth2SuccessHandler;
import com.example.Naengbuhae.controller.IngredientController;
import com.example.Naengbuhae.service.IngredientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// IngredientController를 통해 GlobalExceptionHandler의 각 분기를 검증.
// @AutoConfigureMockMvc(addFilters=false)로 SecurityConfig/JwtFilter 우회.
// Principal은 람다로 주입.
@WebMvcTest(IngredientController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    IngredientService ingredientService;

    // SecurityConfig가 필요로 하는 의존성들 — addFilters=false로 실제 호출은 안 되지만 빈 그래프 구성을 위해 mock 주입
    @MockBean JwtUtil jwtUtil;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean CustomOAuth2UserService customOAuth2UserService;
    @MockBean OAuth2SuccessHandler oAuth2SuccessHandler;

    Principal alice = () -> "alice";

    @Test
    @DisplayName("validation 실패 → 400 + 모든 field 에러를 ';'로 합쳐서 메시지에 포함")
    void validationError_aggregatesAllFieldErrors() throws Exception {
        // 빈 body → @NotBlank/NotNull 다수 발동
        mockMvc.perform(post("/api/ingredients")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString(";"))); // 다중 에러 join
    }

    @Test
    @DisplayName("서비스에서 IllegalArgumentException → 400 + 서비스 메시지 그대로")
    void illegalArgumentFromService_returns400() throws Exception {
        doThrow(new IllegalArgumentException("해당 식재료가 없습니다. id=99"))
                .when(ingredientService).deleteIngredient(eq(99L), eq("alice"));

        mockMvc.perform(delete("/api/ingredients/99").principal(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("해당 식재료")));
    }

    @Test
    @DisplayName("path variable 타입 불일치 (/api/ingredients/abc) → 400 + 'Long 타입' 안내")
    void typeMismatch_pathVariable_returns400() throws Exception {
        mockMvc.perform(delete("/api/ingredients/abc").principal(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Long")));
    }

    @Test
    @DisplayName("malformed JSON → 400 + 'yyyy-MM-dd' 안내 메시지")
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/ingredients")
                        .content("not-valid-json {{{")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("입력값")));
    }

    @Test
    @DisplayName("예기치 못한 RuntimeException → 500 + 일반 메시지 (스택트레이스 응답에 노출 X)")
    void unexpectedException_returns500WithGenericMessage() throws Exception {
        when(ingredientService.findAllIngredients("alice", null))
                .thenThrow(new RuntimeException("내부 디버그용 메시지 — 응답엔 노출 금지"));

        mockMvc.perform(get("/api/ingredients").principal(alice))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("서버에서 오류")));
    }

    @Test
    @DisplayName("정상 흐름 (참고용): 200 OK")
    void successCase() throws Exception {
        when(ingredientService.findAllIngredients("alice", null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/ingredients").principal(alice))
                .andExpect(status().isOk());
    }
}
