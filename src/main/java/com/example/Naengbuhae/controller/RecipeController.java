package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.AiFoodRecommendRequestDto;
import com.example.Naengbuhae.dto.AiRecipeResponseDto;
import com.example.Naengbuhae.dto.RecipeMatchResponseDto;
import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.service.AiRecipeService;
import com.example.Naengbuhae.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final AiRecipeService aiRecipeService; // ✨ 완벽한 의존성 주입

    // POST: 레시피 등록 (로그인한 사용자와 연결)
    @PostMapping
    public Long create(@Valid @RequestBody RecipeRequestDto requestDto, Principal principal) {
        return recipeService.saveRecipe(requestDto, principal.getName());
    }

    // GET: 내 레시피 전체 조회
    @GetMapping
    public List<RecipeResponseDto> list(Principal principal) {
        return recipeService.findAllRecipes(principal.getName());
    }

    // GET: 내 냉장고 재료 기반 레시피 매칭 (모든 레시피를 매칭률과 함께 반환)
    @GetMapping("/recommendations")
    public List<RecipeMatchResponseDto> recommend(
            @RequestParam(required = false) UUID fridgeId,
            Principal principal) {
        return recipeService.recommendRecipes(principal.getName(), fridgeId);
    }

    // PUT: 레시피 수정 (주인만 가능)
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @Valid @RequestBody RecipeRequestDto requestDto, Principal principal) {
        return recipeService.updateRecipe(id, requestDto, principal.getName());
    }

    // DELETE: 레시피 삭제 (주인만 가능)
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        recipeService.deleteRecipe(id, principal.getName());
        return id + "번 레시피가 삭제되었습니다! 🗑️";
    }

    // GET: 외부 AI (Gemini/FastAPI) 기반 맞춤형 레시피 추천 — 냉장고 전체 식재료 기반
    @GetMapping("/ai-recommendations")
    public List<AiRecipeResponseDto> getAiRecommendation(Principal principal) {
        return aiRecipeService.getAiRecommendation(principal.getName());
    }

    // POST: 단일 식재료 + 영양정보 기반 추천 — capstone-ai /fdmake 프록시
    // /api/nutrition/analyze 결과 카드의 "이 재료로 요리 추천" 흐름에서 호출
    @PostMapping("/ai-for-food")
    public List<AiRecipeResponseDto> getAiRecommendationForFood(
            @RequestBody AiFoodRecommendRequestDto request,
            Principal principal) {
        return aiRecipeService.getAiRecommendationForFood(
                principal.getName(),
                request.getFoodName(),
                request.getCat(),
                request.getNutritionData());
    }

    // POST: 즐겨찾기 토글 — 응답으로 새 상태(true=즐겨찾기) 반환.
    @PostMapping("/{id}/favorite/toggle")
    public java.util.Map<String, Boolean> toggleFavorite(@PathVariable Long id, Principal principal) {
        boolean now = recipeService.toggleFavorite(id, principal.getName());
        return java.util.Map.of("favorite", now);
    }
}
