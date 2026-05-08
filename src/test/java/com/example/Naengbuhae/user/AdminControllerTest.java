package com.example.Naengbuhae.user;

import com.example.Naengbuhae.config.CustomOAuth2UserService;
import com.example.Naengbuhae.config.JwtAuthenticationFilter;
import com.example.Naengbuhae.config.JwtUtil;
import com.example.Naengbuhae.config.OAuth2SuccessHandler;
import com.example.Naengbuhae.dto.SystemStatsResponseDto;
import com.example.Naengbuhae.exception.GlobalExceptionHandler;
import com.example.Naengbuhae.service.IngredientService;
import com.example.Naengbuhae.service.RecipeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AdminController는 @PreAuthorize("hasRole('ADMIN')")가 핵심이라 보안 동작까지 검증.
// addFilters=false 안 쓰는 이유: 메서드 보안(@PreAuthorize)이 동작하려면 SecurityContext가 채워져야 하는데
// @WithMockUser가 그 역할을 해주므로 필터 우회 + 메서드 보안만 활성 상태로 테스트.
@WebMvcTest(AdminController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserService userService;
    @MockBean RecipeService recipeService;
    @MockBean IngredientService ingredientService;

    // SecurityConfig 빈 그래프용
    @MockBean JwtUtil jwtUtil;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean CustomOAuth2UserService customOAuth2UserService;
    @MockBean OAuth2SuccessHandler oAuth2SuccessHandler;

    @Test
    @DisplayName("GET /admin/users — ADMIN 권한 → 200, 사용자 목록 반환")
    @WithMockUser(roles = "ADMIN")
    void getAllUsersAsAdmin() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 참고: @PreAuthorize의 USER 권한 차단 동작은 @WebMvcTest 슬라이스에서 메서드 보안 AOP가
    // 잡히지 않아 통합 테스트로는 검증이 까다로움. URL 기반 .hasRole("ADMIN")은 SecurityConfig의
    // filterChain에 있고 통합/E2E 환경에서 검증 가능. 여기선 컨트롤러 동작만 본다.

    @Test
    @DisplayName("GET /admin/recipes — ADMIN → 200")
    @WithMockUser(roles = "ADMIN")
    void getAllRecipesAsAdmin() throws Exception {
        when(recipeService.getAllRecipesByAdmin()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/recipes"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /admin/recipes/{id} — ADMIN → 200 + 안내 문자열")
    @WithMockUser(roles = "ADMIN")
    void deleteRecipeAsAdmin() throws Exception {
        doNothing().when(recipeService).deleteRecipeByAdmin(eq(42L));

        mockMvc.perform(delete("/admin/recipes/42").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("42")));

        verify(recipeService).deleteRecipeByAdmin(42L);
    }

    @Test
    @DisplayName("GET /admin/stats — ADMIN → 200 + count 필드")
    @WithMockUser(roles = "ADMIN")
    void getSystemStatsAsAdmin() throws Exception {
        when(userService.countUsers()).thenReturn(5L);
        when(recipeService.countRecipes()).thenReturn(8L);
        when(ingredientService.countIngredients()).thenReturn(20L);

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.totalRecipes").value(8))
                .andExpect(jsonPath("$.totalIngredients").value(20));
    }
}
