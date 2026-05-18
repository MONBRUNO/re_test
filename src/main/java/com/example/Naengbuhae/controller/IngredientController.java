package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.ExpiringIngredientResponseDto;
import com.example.Naengbuhae.dto.IngredientImportRequestDto;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import com.example.Naengbuhae.user.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

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

    // GET: 식재료 조회. fridgeId 안 주면 사용자의 기본 냉장고.
    @GetMapping
    public List<IngredientResponseDto> list(@RequestParam(required = false) UUID fridgeId,
                                            Principal principal) {
        return ingredientService.findAllIngredients(principal.getName(), fridgeId);
    }

    // GET: 유통기한 임박 식재료 (만료된 것 + 향후 N일 이내 만료)
    // 예: /api/ingredients/expiring?days=3&fridgeId=...
    @GetMapping("/expiring")
    public List<ExpiringIngredientResponseDto> expiring(@RequestParam(defaultValue = "3") int days,
                                                        @RequestParam(required = false) UUID fridgeId,
                                                        Principal principal) {
        return ingredientService.findExpiring(principal.getName(), days, fridgeId);
    }

    // DELETE: 내 식재료만 삭제 가능하도록 수정
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id, Principal principal) {
        ingredientService.deleteIngredient(id, principal.getName());
        return new ApiResponse(true, id + "번 식재료가 삭제되었습니다!");
    }

    // PUT: 내 식재료만 수정 가능하도록 수정
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @Valid @RequestBody IngredientRequestDto requestDto, Principal principal) {
        return ingredientService.updateIngredient(id, requestDto, principal.getName());
    }

    // POST /import: 게스트 → 로그인 전환 시 로컬에 있던 식재료를 일괄 이전.
    //   응답: 저장된 개수.
    @PostMapping("/import")
    public ApiResponse importIngredients(@Valid @RequestBody IngredientImportRequestDto requestDto,
                                         Principal principal) {
        int saved = ingredientService.importIngredients(requestDto, principal.getName());
        return new ApiResponse(true, "식재료 " + saved + "개가 이전되었습니다.");
    }

    // POST /bulk-delete: 다중 선택 → 일괄 삭제. body: {"ids":[1,2,3]}
    //   권한 없는 id는 조용히 건너뛰고, 실제 삭제된 개수를 반환.
    @PostMapping("/bulk-delete")
    public ApiResponse bulkDelete(@RequestBody java.util.Map<String, java.util.List<Long>> body,
                                  Principal principal) {
        int deleted = ingredientService.deleteIngredients(body.get("ids"), principal.getName());
        return new ApiResponse(true, deleted + "개의 식재료가 삭제되었습니다.");
    }
}
