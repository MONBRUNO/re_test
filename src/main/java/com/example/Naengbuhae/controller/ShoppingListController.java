package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.ShoppingItemRequestDto;
import com.example.Naengbuhae.dto.ShoppingItemResponseDto;
import com.example.Naengbuhae.service.ShoppingItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingItemService shoppingItemService;

    // GET: 내 장보기 목록 전체 조회
    @GetMapping
    public List<ShoppingItemResponseDto> getList(Principal principal) {
        return shoppingItemService.getMyShoppingList(principal.getName());
    }

    // POST: 장보기 항목 추가
    @PostMapping
    public ShoppingItemResponseDto addItem(@Valid @RequestBody ShoppingItemRequestDto requestDto, Principal principal) {
        return shoppingItemService.addShoppingItem(requestDto, principal.getName());
    }

    // PATCH: 구매 완료 체크 토글 (상태 변경)
    @PatchMapping("/{id}/toggle")
    public ShoppingItemResponseDto toggleItem(@PathVariable Long id, Principal principal) {
        return shoppingItemService.toggleCheck(id, principal.getName());
    }

    // DELETE: 장보기 항목 삭제
    @DeleteMapping("/{id}")
    public String deleteItem(@PathVariable Long id, Principal principal) {
        shoppingItemService.deleteShoppingItem(id, principal.getName());
        return id + "번 장보기 항목이 삭제되었습니다.";
    }

    // ✨ 추가: 체크 완료된 항목 냉장고로 옮기기 (버튼 클릭용)
    @PostMapping("/move-to-fridge")
    public String moveToFridge(Principal principal) {
        return shoppingItemService.moveCheckedItemsToFridge(principal.getName());
    }

    // 다중 선택 일괄 삭제. body: {"ids": [1, 2, 3]}
    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestBody java.util.Map<String, java.util.List<Long>> body,
                             Principal principal) {
        int deleted = shoppingItemService.deleteShoppingItems(body.get("ids"), principal.getName());
        return deleted + "개의 장보기 항목이 삭제되었습니다.";
    }

    // 일괄 추가 — 레시피 상세 "부족한 재료 추가" 등. body: {"items": [{name, quantity, unit}, ...]}
    @PostMapping("/bulk-add")
    public String bulkAdd(@Valid @RequestBody java.util.Map<String, java.util.List<ShoppingItemRequestDto>> body,
                          Principal principal) {
        int added = shoppingItemService.addShoppingItems(body.get("items"), principal.getName());
        return added + "개의 장보기 항목이 추가되었습니다.";
    }

    // 자동 제안 — 가족이 자주 비웠는데 지금 냉장고에도 없고 장보기에도 없는 식재료.
    @GetMapping("/suggestions")
    public java.util.List<java.util.Map<String, Object>> suggestions(
            @RequestParam(required = false) UUID fridgeId,
            @RequestParam(defaultValue = "5") int limit,
            Principal principal) {
        return shoppingItemService.getSuggestions(principal.getName(), fridgeId, limit);
    }
}
