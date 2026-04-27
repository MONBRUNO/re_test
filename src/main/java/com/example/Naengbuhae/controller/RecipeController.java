package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    // POST: 레시피 등록 (로그인한 사용자와 연결)
    @PostMapping
    public Long create(@RequestBody RecipeRequestDto requestDto, Principal principal) {
        return recipeService.saveRecipe(requestDto, principal.getName());
    }

    // GET: 내 레시피 전체 조회
    @GetMapping
    public List<RecipeResponseDto> list(Principal principal) {
        return recipeService.findAllRecipes(principal.getName());
    }

    // PUT: 레시피 수정 (주인만 가능)
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @RequestBody RecipeRequestDto requestDto, Principal principal) {
        return recipeService.updateRecipe(id, requestDto, principal.getName());
    }

    // DELETE: 레시피 삭제 (주인만 가능)
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        recipeService.deleteRecipe(id, principal.getName());
        return id + "번 레시피가 삭제되었습니다! 🗑️";
    }
}
