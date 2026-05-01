package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.ShoppingItemRequestDto;
import com.example.Naengbuhae.dto.ShoppingItemResponseDto;
import com.example.Naengbuhae.service.ShoppingItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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
}