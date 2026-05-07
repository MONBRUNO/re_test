package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.RecipeMatchResponseDto;
import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
// ✨ 새로 만든 DTO와 Service를 import!
import com.example.Naengbuhae.dto.AiRecipeResponseDto;
import com.example.Naengbuhae.service.RecipeService;
import com.example.Naengbuhae.service.AiRecipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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
    public List<RecipeMatchResponseDto> recommend(Principal principal) {
        return recipeService.recommendRecipes(principal.getName());
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

    // ✨ AI 레시피 추천 API 엔드포인트
    @GetMapping("/ai-recommendations")
    public AiRecipeResponseDto getAiRecommend(Principal principal) {
        return aiRecipeService.getAiRecommendation(principal.getName());
    }
}