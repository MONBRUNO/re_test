package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.ShoppingItem;
import com.example.Naengbuhae.dto.ShoppingItemRequestDto;
import com.example.Naengbuhae.dto.ShoppingItemResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository; // ✨ 냉장고 창고 추가!
import com.example.Naengbuhae.repository.ShoppingItemRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShoppingItemService {

    private final ShoppingItemRepository shoppingItemRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository; // ✨ 냉장고 저장용 의존성 주입

    // 1. 장보기 항목 추가
    @Transactional
    public ShoppingItemResponseDto addShoppingItem(ShoppingItemRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        ShoppingItem shoppingItem = requestDto.toEntity(user);
        ShoppingItem savedItem = shoppingItemRepository.save(shoppingItem);

        return new ShoppingItemResponseDto(savedItem);
    }

    // 2. 내 장보기 목록 전체 조회
    public List<ShoppingItemResponseDto> getMyShoppingList(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        return shoppingItemRepository.findByUser(user).stream()
                .map(ShoppingItemResponseDto::new)
                .collect(Collectors.toList());
    }

    // 3. 구매 완료 체크 토글 (true <-> false)
    @Transactional
    public ShoppingItemResponseDto toggleCheck(Long id, String username) {
        ShoppingItem shoppingItem = shoppingItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 장보기 항목이 없습니다."));

        // 내 항목이 맞는지 확인! (보안 철저)
        if (!shoppingItem.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 장보기 항목만 상태를 변경할 수 있습니다.");
        }

        // 현재 상태를 반대로 뒤집기 (true면 false로, false면 true로)
        shoppingItem.setChecked(!shoppingItem.isChecked());

        return new ShoppingItemResponseDto(shoppingItem);
    }

    // 4. 장보기 항목 삭제
    @Transactional
    public void deleteShoppingItem(Long id, String username) {
        ShoppingItem shoppingItem = shoppingItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 장보기 항목이 없습니다."));

        // 내 항목이 맞는지 확인!
        if (!shoppingItem.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 장보기 항목만 삭제할 수 있습니다.");
        }
        shoppingItemRepository.delete(shoppingItem);
    }
    // ✨ 5. 마법의 API: 구매 완료된 항목을 냉장고로 옮기기!
    @Transactional
    public String moveCheckedItemsToFridge(String username) {
        // 1. 유저 확인
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        // 2. 장바구니에서 '체크 완료(true)'된 항목들만 불러오기
        List<ShoppingItem> purchasedItems = shoppingItemRepository.findByUserAndCheckedTrue(user);

        // 체크된 게 하나도 없으면 튕겨내기
        if (purchasedItems.isEmpty()) {
            return "냉장고로 옮길 항목이 없습니다. 장보기 목록에서 먼저 체크해주세요!";
        }

        // 3. 장바구니 항목(ShoppingItem)을 냉장고 식재료(Ingredient)로 변환!
        // moveCheckedItemsToFridge 메서드 내부 수정
        List<Ingredient> ingredientsToSave = purchasedItems.stream().map(item -> {
            // ✨ 수정: 소수점 버리지 않고 그대로 가져오기! (null 방어 포함)
            Double finalQuantity = item.getQuantity() != null ? item.getQuantity() : 1.0;

            String unit = (item.getUnit() != null && !item.getUnit().isBlank()) ? item.getUnit() : "개";

            return new Ingredient(
                    user,
                    item.getName(),
                    finalQuantity, // Integer 대신 Double 값 삽입
                    LocalDate.now().plusDays(7),
                    "미분류",
                    unit,
                    "냉장",
                    LocalDate.now()
            );
        }).collect(Collectors.toList());
        // 4. 변환된 식재료들을 내 냉장고 DB에 한 방에 쾅! 저장
        ingredientRepository.saveAll(ingredientsToSave);

        // 5. 냉장고로 들어갔으니, 기존 장바구니 리스트에서는 일괄 삭제 (깔끔하게 청소)
        shoppingItemRepository.deleteAll(purchasedItems);

        return purchasedItems.size() + "개의 항목이 냉장고로 쏙! 들어갔습니다. 🧊";
    }
}
