package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    // POST: 저장 요청이 오면 '받는 택배 상자(RequestDto)'로 안전하게 받기
    @PostMapping
    public Long create(@RequestBody IngredientRequestDto requestDto) {
        return ingredientService.saveIngredient(requestDto);
    }

    // GET: 전체 조회 요청이 오면 원본 말고 '보내는 택배 상자(ResponseDto)' 리스트로 안전하게 내보내기
    @GetMapping
    public List<IngredientResponseDto> list() {
        return ingredientService.findAllIngredients();
    }

    // --- API 3: 식재료 삭제하기 (DELETE 요청) ---
    // @DeleteMapping: 누군가 주소 뒤에 번호(id)를 달고 DELETE 요청을 보내면 실행됨
    // 예: /api/ingredients/1 (1번 지워줘!)
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return id + "번 식재료가 냉장고에서 삭제되었습니다! 🗑️";
    }

    // --- API 4: 식재료 수정하기 (PUT 요청) ---
    // @PutMapping: 누군가 주소 뒤에 번호(id)를 달고 PUT(수정) 요청을 보내면 실행됨
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @RequestBody IngredientRequestDto requestDto) {
        // 두뇌(Service)에게 "id번 식재료를 이 새 정보(requestDto)로 바꿔줘!" 라고 시킴
        return ingredientService.updateIngredient(id, requestDto);
    }
}