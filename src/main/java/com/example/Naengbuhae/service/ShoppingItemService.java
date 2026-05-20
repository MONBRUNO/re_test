package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.ActivityLog;
import com.example.Naengbuhae.domain.Category;
import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.ShoppingItem;
import com.example.Naengbuhae.domain.Storage;
import com.example.Naengbuhae.dto.ShoppingItemRequestDto;
import com.example.Naengbuhae.dto.ShoppingItemResponseDto;
import com.example.Naengbuhae.repository.ActivityLogRepository;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository; // ✨ 냉장고 창고 추가!
import com.example.Naengbuhae.repository.ShoppingItemRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShoppingItemService {

    private final ShoppingItemRepository shoppingItemRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository; // ✨ 냉장고 저장용 의존성 주입
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final ActivityLogService activityLogService;
    private final ActivityLogRepository activityLogRepository; // ✨ 통계용 의존성 복구

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
                .toList();
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

    // 4-4. 일괄 추가 — 레시피 상세에서 "부족한 재료 한 번에 담기"용.
    //      이미 같은 이름이 장보기에 있으면 중복 추가 안 함 (UX상 N번 추가는 의미 없음).
    @Transactional
    public int addShoppingItems(List<ShoppingItemRequestDto> items, String username) {
        if (items == null || items.isEmpty()) return 0;
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        Set<String> existing = shoppingItemRepository.findByUser(user).stream()
                .map(ShoppingItem::getName)
                .collect(Collectors.toCollection(HashSet::new));

        int added = 0;
        for (ShoppingItemRequestDto dto : items) {
            if (existing.contains(dto.getName())) continue;
            shoppingItemRepository.save(dto.toEntity(user));
            existing.add(dto.getName());
            added++;
        }
        return added;
    }

    // 4-3. 장보기 자동 제안 — 가족이 자주 비운(INGREDIENT_REMOVED) 식재료 중
    //      "현재 냉장고에 없고" + "이미 장보기에 없는" 이름만 골라서 카운트 내림차순으로 반환.
    //      기간은 최근 60일.
    public List<Map<String, Object>> getSuggestions(String username, UUID fridgeId, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));
        Fridge fridge = resolveFridge(user, fridgeId);

        LocalDateTime since = LocalDateTime.now().minusDays(60);
        List<Object[]> rows = activityLogRepository.topIngredientsByAction(
                fridge, ActivityLog.Action.INGREDIENT_REMOVED, since);

        Set<String> inFridge = ingredientRepository.findByFridge(fridge).stream()
                .map(Ingredient::getName)
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> inShopping = shoppingItemRepository.findByUser(user).stream()
                .map(ShoppingItem::getName)
                .collect(Collectors.toCollection(HashSet::new));

        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String name = (String) row[0];
            long count = (long) row[1];
            if (inFridge.contains(name) || inShopping.contains(name)) continue;
            result.add(Map.of("name", name, "count", count));
            if (result.size() >= safeLimit) break;
        }
        return result;
    }

    // 사용자가 멤버인 냉장고 결정. fridgeId 명시되면 그 냉장고(권한 검증), 아니면 첫 냉장고.
    private Fridge resolveFridge(User user, UUID fridgeId) {
        if (fridgeId != null) {
            Fridge fridge = fridgeRepository.findById(fridgeId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다."));
            if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, user)) {
                throw new IllegalArgumentException("이 냉장고에 접근 권한이 없습니다.");
            }
            return fridge;
        }
        return fridgeRepository.findAllForMember(user).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("냉장고가 없습니다."));
    }

    // 4-2. 다중 선택 일괄 삭제. 본인 소유 항목만 삭제, 그 외 id는 조용히 무시.
    @Transactional
    public int deleteShoppingItems(List<Long> ids, String username) {
        if (ids == null || ids.isEmpty()) return 0;
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        List<ShoppingItem> targets = shoppingItemRepository.findAllById(ids).stream()
                .filter(it -> it.getUser().getUsername().equals(user.getUsername()))
                .toList();
        if (targets.isEmpty()) return 0;
        shoppingItemRepository.deleteAll(targets);
        return targets.size();
    }
    // 5-2. 단일 장보기 항목을 냉장고로 이관 (체크 여부 무관). 본인 소유 검증 후
    //      moveCheckedItemsToFridge와 동일 패턴으로 Ingredient 생성 + 활동로그 + ShoppingItem 삭제.
    @Transactional
    public String transferToFridge(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));
        ShoppingItem item = shoppingItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 장보기 항목이 없습니다."));
        if (!item.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 장보기 항목만 이관할 수 있습니다.");
        }

        Fridge defaultFridge = fridgeRepository.findAllForMember(user).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("참여 중인 냉장고가 없습니다. 냉장고를 먼저 생성해주세요."));

        Double finalQuantity = item.getQuantity() != null ? item.getQuantity() : 1.0;
        String unit = (item.getUnit() != null && !item.getUnit().isBlank()) ? item.getUnit() : "개";

        Ingredient ingredient = new Ingredient(
                user,
                item.getName(),
                finalQuantity,
                LocalDate.now().plusDays(7),
                Category.ETC,
                unit,
                Storage.REFRIGERATED,
                LocalDate.now()
        );
        ingredient.setFridge(defaultFridge);
        ingredientRepository.save(ingredient);

        activityLogService.recordIngredientAdded(defaultFridge, user, item.getName(), finalQuantity, unit);
        shoppingItemRepository.delete(item);

        return item.getName() + " 항목이 '" + defaultFridge.getName() + "' 냉장고로 이동되었습니다.";
    }

    // ✨ 5. 마법의 API: 구매 완료된 항목을 냉장고로 옮기기!
    @Transactional
    public String moveCheckedItemsToFridge(String username) {
        // 1. 유저 확인
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        // ✨ 2. 이 유저가 속한 냉장고 목록 중 첫 번째(기본 냉장고)를 확보합니다.
        // 🚨 이 로직이 들어가야 고아 데이터가 되지 않고 공유 냉장고에 딱 꽂힙니다!
        Fridge defaultFridge = fridgeRepository.findAllForMember(user).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("참여 중인 냉장고가 없습니다. 냉장고를 먼저 생성해주세요."));

        // 3. 장바구니에서 '체크 완료(true)'된 항목들만 불러오기
        List<ShoppingItem> purchasedItems = shoppingItemRepository.findByUserAndCheckedTrue(user);

        // 체크된 게 하나도 없으면 튕겨내기
        if (purchasedItems.isEmpty()) {
            return "냉장고로 옮길 항목이 없습니다. 장보기 목록에서 먼저 체크해주세요!";
        }

        // 4. 장바구니 항목(ShoppingItem)을 냉장고 식재료(Ingredient)로 변환!
        for (ShoppingItem item : purchasedItems) {
            Double finalQuantity = item.getQuantity() != null ? item.getQuantity() : 1.0;
            String unit = (item.getUnit() != null && !item.getUnit().isBlank()) ? item.getUnit() : "개";

            Ingredient ingredient = new Ingredient(
                    user,
                    item.getName(),
                    finalQuantity,
                    LocalDate.now().plusDays(7), // 기본 유통기한 일주일 부여
                    Category.ETC, // 장보기 항목에는 카테고리가 없으므로 기타로 설정
                    unit,
                    Storage.REFRIGERATED,
                    LocalDate.now()
            );

            // ✨ 냉장고 연결!
            ingredient.setFridge(defaultFridge);
            ingredientRepository.save(ingredient);

            // 활동 로그 남기기 (가족 통계용)
            activityLogService.recordIngredientAdded(defaultFridge, user, item.getName(), finalQuantity, unit);
        }

        // 5. 냉장고로 들어갔으니, 기존 장바구니 리스트에서는 일괄 삭제 (벌크 삭제로 성능 최적화)
        shoppingItemRepository.deleteAllInBatch(purchasedItems);

        return purchasedItems.size() + "개의 항목이 '" + defaultFridge.getName() + "' 냉장고로 안전하게 이동되었습니다! 🧊";
    }
}
