package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.config.CustomOAuth2UserService;
import com.example.Naengbuhae.config.JwtAuthenticationFilter;
import com.example.Naengbuhae.config.JwtUtil;
import com.example.Naengbuhae.config.OAuth2SuccessHandler;
import com.example.Naengbuhae.dto.ExpiringIngredientResponseDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.exception.GlobalExceptionHandler;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// IngredientController happy path 검증.
// 예외 케이스(검증 실패/타입 불일치/JSON 파싱 실패/catch-all)는
// GlobalExceptionHandlerIntegrationTest가 이미 다루므로 여기서는 정상 흐름만.
@WebMvcTest(IngredientController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class IngredientControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean IngredientService ingredientService;

    // SecurityConfig 빈 그래프용
    @MockBean JwtUtil jwtUtil;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean CustomOAuth2UserService customOAuth2UserService;
    @MockBean OAuth2SuccessHandler oAuth2SuccessHandler;

    Principal alice = () -> "alice";

    @Test
    @DisplayName("POST /api/ingredients → 응답에 IngredientResponseDto (allergyWarnings 포함)")
    void create() throws Exception {
        IngredientResponseDto saved = org.mockito.Mockito.mock(IngredientResponseDto.class);
        when(saved.getId()).thenReturn(11L);
        when(saved.getName()).thenReturn("땅콩잼");
        when(saved.getAllergyWarnings()).thenReturn(List.of("땅콩"));
        when(ingredientService.saveIngredient(any(), eq("alice"))).thenReturn(saved);

        mockMvc.perform(post("/api/ingredients")
                        .content(mapper.writeValueAsString(validIngredient()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.name").value("땅콩잼"))
                .andExpect(jsonPath("$.allergyWarnings[0]").value("땅콩"));
    }

    @Test
    @DisplayName("GET /api/ingredients → 사용자 식재료 목록")
    void list() throws Exception {
        IngredientResponseDto a = org.mockito.Mockito.mock(IngredientResponseDto.class);
        when(a.getName()).thenReturn("우유");
        IngredientResponseDto b = org.mockito.Mockito.mock(IngredientResponseDto.class);
        when(b.getName()).thenReturn("계란");
        when(ingredientService.findAllIngredients("alice", null)).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/ingredients").principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("우유"))
                .andExpect(jsonPath("$[1].name").value("계란"));
    }

    @Test
    @DisplayName("GET /api/ingredients/expiring (default days=3) → findExpiring(alice, 3, null) 호출")
    void expiringDefault() throws Exception {
        when(ingredientService.findExpiring("alice", 3, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/ingredients/expiring").principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(ingredientService).findExpiring("alice", 3, null);
    }

    @Test
    @DisplayName("GET /api/ingredients/expiring?days=7 → findExpiring(alice, 7, null) 호출")
    void expiringExplicitDays() throws Exception {
        ExpiringIngredientResponseDto dto = org.mockito.Mockito.mock(ExpiringIngredientResponseDto.class);
        when(dto.getName()).thenReturn("우유");
        when(ingredientService.findExpiring("alice", 7, null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/ingredients/expiring?days=7").principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("우유"));

        verify(ingredientService).findExpiring("alice", 7, null);
    }

    @Test
    @DisplayName("PUT /api/ingredients/{id} → updateIngredient 호출, 수정된 id 반환")
    void update() throws Exception {
        when(ingredientService.updateIngredient(eq(5L), any(), eq("alice"))).thenReturn(5L);

        mockMvc.perform(put("/api/ingredients/5")
                        .content(mapper.writeValueAsString(validIngredient()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(alice))
                .andExpect(status().isOk())
                .andExpect(content().json("5"));
    }

    @Test
    @DisplayName("DELETE /api/ingredients/{id} → deleteIngredient 호출, 안내 문자열에 id 포함")
    void deleteById() throws Exception {
        doNothing().when(ingredientService).deleteIngredient(eq(7L), eq("alice"));

        mockMvc.perform(delete("/api/ingredients/7").principal(alice))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("7")));

        verify(ingredientService).deleteIngredient(7L, "alice");
    }

    private Map<String, Object> validIngredient() {
        return Map.ofEntries(
                Map.entry("name", "땅콩잼"),
                Map.entry("quantity", 1.0),
                Map.entry("expirationDate", LocalDate.now().plusDays(30).toString()),
                Map.entry("category", "기타"),
                Map.entry("unit", "개"),
                Map.entry("storage", "냉장"),
                Map.entry("purchaseDate", LocalDate.now().toString())
        );
    }
}
