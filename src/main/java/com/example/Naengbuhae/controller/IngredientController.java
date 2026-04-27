package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    // POST: 저장할 때 현재 로그인한 사용자의 정보를 Principal에서 가져옴
    @PostMapping
    public Long create(@Valid @RequestBody IngredientRequestDto requestDto, Principal principal) {
        return ingredientService.saveIngredient(requestDto, principal.getName());
    }

    // GET: 내 식재료만 조회
    @GetMapping
    public List<IngredientResponseDto> list(Principal principal) {
        return ingredientService.findAllIngredients(principal.getName());
    }

    // DELETE: 내 식재료만 삭제 가능하도록 수정
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        ingredientService.deleteIngredient(id, principal.getName());
        return id + "번 식재료가 삭제되었습니다!";
    }

    // PUT: 내 식재료만 수정 가능하도록 수정
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @Valid @RequestBody IngredientRequestDto requestDto, Principal principal) {
        return ingredientService.updateIngredient(id, requestDto, principal.getName());
    }
}
