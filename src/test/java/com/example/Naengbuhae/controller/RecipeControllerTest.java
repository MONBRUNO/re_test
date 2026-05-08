package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.config.CustomOAuth2UserService;
import com.example.Naengbuhae.config.JwtAuthenticationFilter;
import com.example.Naengbuhae.config.JwtUtil;
import com.example.Naengbuhae.config.OAuth2SuccessHandler;
import com.example.Naengbuhae.dto.RecipeMatchResponseDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.exception.GlobalExceptionHandler;
import com.example.Naengbuhae.service.AiRecipeService;
import com.example.Naengbuhae.service.RecipeService;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class RecipeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean RecipeService recipeService;
    // AI 레시피 추천은 다른 담당자 영역이라 동작 검증 X — bean 주입만 만족시킴
    @MockBean AiRecipeService aiRecipeService;

    // SecurityConfig 빈 그래프용
    @MockBean JwtUtil jwtUtil;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean CustomOAuth2UserService customOAuth2UserService;
    @MockBean OAuth2SuccessHandler oAuth2SuccessHandler;

    Principal alice = () -> "alice";

    @Test
    @DisplayName("POST /api/recipes → 생성된 id 반환")
    void create() throws Exception {
        when(recipeService.saveRecipe(any(), eq("alice"))).thenReturn(42L);

        mockMvc.perform(post("/api/recipes")
                        .content(mapper.writeValueAsString(validRecipeRequest()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(alice))
                .andExpect(status().isOk())
                .andExpect(content().json("42"));
    }

    @Test
    @DisplayName("GET /api/recipes → 내 레시피 목록")
    void list() throws Exception {
        RecipeResponseDto dto = org.mockito.Mockito.mock(RecipeResponseDto.class);
        when(dto.getName()).thenReturn("계란말이");
        when(recipeService.findAllRecipes("alice")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/recipes").principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("계란말이"));
    }

    @Test
    @DisplayName("GET /api/recipes/recommendations → 매칭률 + 알레르기 필터된 추천 목록")
    void recommendations() throws Exception {
        RecipeResponseDto recipeDto = org.mockito.Mockito.mock(RecipeResponseDto.class);
        when(recipeDto.getName()).thenReturn("계란우유");
        RecipeMatchResponseDto match = new RecipeMatchResponseDto(recipeDto, 100, List.of("계란"), List.of());
        when(recipeService.recommendRecipes("alice")).thenReturn(List.of(match));

        mockMvc.perform(get("/api/recipes/recommendations").principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchRate").value(100))
                .andExpect(jsonPath("$[0].recipe.name").value("계란우유"));
    }

    @Test
    @DisplayName("PUT /api/recipes/{id} → updateRecipe 호출, 수정된 id 반환")
    void update() throws Exception {
        when(recipeService.updateRecipe(eq(7L), any(), eq("alice"))).thenReturn(7L);

        mockMvc.perform(put("/api/recipes/7")
                        .content(mapper.writeValueAsString(validRecipeRequest()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(alice))
                .andExpect(status().isOk())
                .andExpect(content().json("7"));
    }

    @Test
    @DisplayName("DELETE /api/recipes/{id} → deleteRecipe 호출, 안내 문자열 반환")
    void delete_recipe() throws Exception {
        doNothing().when(recipeService).deleteRecipe(eq(99L), eq("alice"));

        mockMvc.perform(delete("/api/recipes/99").principal(alice))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("99")));

        verify(recipeService).deleteRecipe(99L, "alice");
    }

    @Test
    @DisplayName("POST /api/recipes 유효성 위반 (이름 빈 값) → 400")
    void createValidationError() throws Exception {
        Map<String, Object> bad = Map.ofEntries(
                Map.entry("name", ""),         // @NotBlank 위반
                Map.entry("cookingTime", 10)   // 다른 필드는 정상값
        );

        mockMvc.perform(post("/api/recipes")
                        .content(mapper.writeValueAsString(bad))
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(alice))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Map<String, Object> validRecipeRequest() {
        return Map.ofEntries(
                Map.entry("name", "계란말이"),
                Map.entry("category", "반찬"),
                Map.entry("difficulty", "easy"),
                Map.entry("cookingTime", 10),
                Map.entry("servings", 2),
                Map.entry("steps", List.of("계란 풀기", "팬에 익히기")),
                Map.entry("ingredients", List.of(
                        Map.of("name", "계란", "quantity", 3.0, "unit", "개", "required", true)
                ))
        );
    }

    // import for content() — at bottom for clarity
    private static org.springframework.test.web.servlet.result.ContentResultMatchers content() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content();
    }
}
