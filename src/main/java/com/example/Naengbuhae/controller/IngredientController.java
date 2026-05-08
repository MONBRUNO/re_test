package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.ExpiringIngredientResponseDto;
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

    // POST: 저장할 때 현재 로그인한 사용자의 정보를 Principal에서 가져옴.
    // 응답에 allergyWarnings를 포함해 등록 직후 알레르기 매칭 안내가 가능 (이전엔 Long ID만 반환)
    @PostMapping
    public IngredientResponseDto create(@Valid @RequestBody IngredientRequestDto requestDto, Principal principal) {
        return ingredientService.saveIngredient(requestDto, principal.getName());
    }

    // GET: 내 식재료만 조회
    @GetMapping
    public List<IngredientResponseDto> list(Principal principal) {
        return ingredientService.findAllIngredients(principal.getName());
    }

    // GET: 유통기한 임박 식재료 (만료된 것 + 향후 N일 이내 만료)
    // 예: /api/ingredients/expiring?days=3 (기본 3일)
    @GetMapping("/expiring")
    public List<ExpiringIngredientResponseDto> expiring(@RequestParam(defaultValue = "3") int days,
                                                        Principal principal) {
        return ingredientService.findExpiring(principal.getName(), days);
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
