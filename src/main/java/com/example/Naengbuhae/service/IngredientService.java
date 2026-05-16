package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.ExpiringIngredientResponseDto;
import com.example.Naengbuhae.dto.IngredientImportRequestDto;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.example.Naengbuhae.user.UserService;
import com.example.Naengbuhae.util.AllergyMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final AppNotificationService appNotificationService;
    private final ActivityLogService activityLogService;

    // === 헬퍼: 요청 시 활성 냉장고 결정 ===
    // fridgeId가 명시되면 그 냉장고. 없으면 사용자가 멤버인 첫 번째 냉장고(=기본 냉장고).
    // 어느 쪽이든 사용자가 멤버여야 한다.
    // 사용자에게 냉장고가 하나도 없으면(신규 가입 / 마이그레이션 누락) 자동으로 "내 냉장고" 생성.
    private Fridge resolveFridge(User user, Long fridgeId) {
        if (fridgeId != null) {
            Fridge fridge = fridgeRepository.findById(fridgeId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다. id=" + fridgeId));
            if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, user)) {
                throw new IllegalArgumentException("이 냉장고에 접근 권한이 없습니다.");
            }
            return fridge;
        }
        return fridgeRepository.findAllForMember(user).stream().findFirst()
                .orElseGet(() -> ensureDefaultFridge(user));
    }

    // 기본 냉장고 자동 생성 (안전망). 이름은 "<사용자이름>의 냉장고".
    private Fridge ensureDefaultFridge(User user) {
        Fridge fridge = fridgeRepository.save(new Fridge(user, UserService.defaultFridgeName(user)));
        fridgeMemberRepository.save(new FridgeMember(fridge, user));
        return fridge;
    }

    // === 헬퍼: 특정 식재료에 사용자가 접근 권한이 있는지(=그 냉장고 멤버인지) ===
    private void assertMember(Ingredient ingredient, User user) {
        Fridge fridge = ingredient.getFridge();
        if (fridge == null) {
            // 마이그레이션 누락 케이스: 본인이 추가한 식재료만 본인이 접근 가능
            if (!ingredient.getUser().getUsername().equals(user.getUsername())) {
                throw new IllegalArgumentException("이 식재료에 접근 권한이 없습니다.");
            }
            return;
        }
        if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, user)) {
            throw new IllegalArgumentException("이 식재료에 접근 권한이 없습니다.");
        }
    }

    // 1. 저장 — 사용자가 멤버인 냉장고에 추가. fridgeId 없으면 기본 냉장고.
    @Transactional
    public IngredientResponseDto saveIngredient(IngredientRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        Fridge fridge = resolveFridge(user, requestDto.getFridgeId());
        Ingredient entity = requestDto.toEntity(user);
        entity.setFridge(fridge);

        Ingredient saved = ingredientRepository.save(entity);

        // 활동 로그 (가족 활동 통계용)
        activityLogService.recordIngredientAdded(
                fridge, user, saved.getName(), saved.getQuantity(), saved.getUnit());

        // 같은 냉장고 다른 멤버에게 알림 (혼자 쓰는 냉장고면 발송 대상 없음)
        // 추가 알림은 ingredient:{id} 로 — 알림 탭 시 해당 식재료 수정 화면으로 직행
        notifyOtherMembers(fridge, user,
                "식재료가 추가됐어요",
                user.getName() + "님이 '" + fridge.getName() + "'에 "
                        + saved.getName() + " " + saved.getQuantity() + saved.getUnit() + "를(을) 넣었어요.",
                "ingredient:" + saved.getId());

        Set<String> allergens = AllergyMatcher.parseAllergens(user.getAllergies());
        return toResponseWithAllergyWarnings(saved, allergens);
    }

    // 2. 조회 — 특정 냉장고의 식재료. fridgeId 안 주면 기본 냉장고.
    //    가족 공유 시 같은 냉장고 멤버 모두 동일 결과 반환.
    public List<IngredientResponseDto> findAllIngredients(String username, Long fridgeId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        Fridge fridge = resolveFridge(user, fridgeId);
        Set<String> allergens = AllergyMatcher.parseAllergens(user.getAllergies());

        return ingredientRepository.findByFridge(fridge).stream()
                .map(ing -> toResponseWithAllergyWarnings(ing, allergens))
                .toList();
    }

    // 기존 시그니처 호환용 (다른 서비스가 이미 쓰고 있을 수 있음)
    public List<IngredientResponseDto> findAllIngredients(String username) {
        return findAllIngredients(username, null);
    }

    // 식재료 → DTO 변환 + 알레르기 매칭 결과 채움
    private IngredientResponseDto toResponseWithAllergyWarnings(Ingredient ingredient, Set<String> allergens) {
        IngredientResponseDto dto = new IngredientResponseDto(ingredient);
        if (!allergens.isEmpty()) {
            dto.setAllergyWarnings(new ArrayList<>(
                    AllergyMatcher.findMatches(allergens, Collections.singletonList(ingredient.getName()))
            ));
        }
        return dto;
    }

    // 3. 식재료 삭제 — 냉장고 멤버 누구나 가능 (공유 단위가 냉장고이므로).
    @Transactional
    public void deleteIngredient(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 식재료가 없습니다. id=" + id));
        assertMember(ingredient, user);

        Fridge fridge = ingredient.getFridge();
        String ingName = ingredient.getName();
        ingredientRepository.delete(ingredient);

        // 공유 냉장고에서만 알림/로그 — 마이그레이션 누락 케이스(fridge null)는 본인만 보유 중이므로 스킵
        // 삭제 알림은 ingredient 자체가 사라졌으므로 일반 목록으로만 라우팅
        if (fridge != null) {
            activityLogService.recordIngredientRemoved(fridge, user, ingName);
            notifyOtherMembers(fridge, user,
                    "식재료가 사라졌어요",
                    user.getName() + "님이 '" + fridge.getName() + "'에서 " + ingName + "를(을) 비웠어요.",
                    "ingredients");
        }
    }

    // 3-2. 일괄 삭제 — 여러 식재료 한 번에. 멤버가 아닌 식재료는 자동으로 건너뛴다.
    //      알림은 냉장고별로 1회만 묶어서 보내고, 활동 로그는 식재료별로 정상 기록한다.
    @Transactional
    public int deleteIngredients(java.util.List<Long> ids, String username) {
        if (ids == null || ids.isEmpty()) return 0;
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        // 멤버 권한 있는 식재료만 추려서 삭제 — 권한 없는 id는 조용히 무시 (UX상 부분 실패 응답이 더 혼란).
        java.util.List<Ingredient> targets = new java.util.ArrayList<>();
        for (Long id : ids) {
            Ingredient ing = ingredientRepository.findById(id).orElse(null);
            if (ing == null) continue;
            Fridge f = ing.getFridge();
            if (f != null && !fridgeMemberRepository.existsByFridgeAndUser(f, user)) continue;
            if (f == null && !ing.getUser().getUsername().equals(user.getUsername())) continue;
            targets.add(ing);
        }
        if (targets.isEmpty()) return 0;

        // 냉장고별로 그룹핑 — 알림은 fridge당 1회로 묶고, 활동 로그는 식재료별로 기록.
        java.util.Map<Fridge, java.util.List<String>> byFridge = new java.util.LinkedHashMap<>();
        for (Ingredient ing : targets) {
            Fridge f = ing.getFridge();
            String name = ing.getName();
            if (f != null) {
                activityLogService.recordIngredientRemoved(f, user, name);
                byFridge.computeIfAbsent(f, k -> new java.util.ArrayList<>()).add(name);
            }
        }

        // ✨ 최적화 완료: 루프 삭제가 아닌 벌크 삭제(1번의 쿼리)로 DB 부하 획기적 절감
        ingredientRepository.deleteAllInBatch(targets);

        for (var e : byFridge.entrySet()) {
            Fridge f = e.getKey();
            java.util.List<String> names = e.getValue();
            // "사과 외 N개" 식으로 짧게 요약. 단일 항목이어도 동일한 분기 사용.
            String preview = names.get(0) + (names.size() > 1 ? " 외 " + (names.size() - 1) + "개" : "");
            // 일괄 삭제 — N개 모두 사라졌으므로 단일 식재료 deep link 의미 없음, 목록으로
            notifyOtherMembers(f, user,
                    "식재료가 사라졌어요",
                    user.getName() + "님이 '" + f.getName() + "'에서 " + preview + "를(을) 비웠어요.",
                    "ingredients");
        }
        return targets.size();
    }

    // 4. 식재료 수정 — 냉장고 멤버 누구나 가능.
    //    fridgeId가 dto에 명시되면 다른 냉장고로 이동도 허용 (대상 냉장고도 멤버여야 함).
    @Transactional
    public Long updateIngredient(Long id, IngredientRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 식재료가 없습니다. id=" + id));
        assertMember(ingredient, user);

        ingredient.setName(requestDto.getName());
        ingredient.setQuantity(requestDto.getQuantity());
        ingredient.setExpirationDate(requestDto.getExpirationDate());
        ingredient.setCategory(requestDto.getCategory());
        ingredient.setUnit(requestDto.getUnit());
        ingredient.setStorage(requestDto.getStorage());
        ingredient.setPurchaseDate(requestDto.getPurchaseDate());

        // 냉장고 이동 (다른 냉장고로 옮기기). null이면 유지.
        if (requestDto.getFridgeId() != null &&
                (ingredient.getFridge() == null || !requestDto.getFridgeId().equals(ingredient.getFridge().getId()))) {
            Fridge target = resolveFridge(user, requestDto.getFridgeId());
            ingredient.setFridge(target);
        }

        return ingredient.getId();
    }

    public long countIngredients() {
        return ingredientRepository.count();
    }

    // 6. 일괄 import — 게스트 → 로그인 전환 시 로컬 데이터를 한 번에 옮긴다.
    //    사용자의 기본 냉장고에 추가. 가족 알림/활동 로그는 스킵 (마이그레이션은 의미 없는 노이즈).
    //    반환값: 실제로 저장된 개수.
    @Transactional
    public int importIngredients(IngredientImportRequestDto request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        Fridge fridge = resolveFridge(user, null);

        int saved = 0;
        for (IngredientImportRequestDto.Item item : request.getItems()) {
            Ingredient entity = new Ingredient(
                    user, item.getName(), item.getQuantity(), item.getExpirationDate(),
                    item.getCategory(), item.getUnit(), item.getStorage(), item.getPurchaseDate());
            entity.setFridge(fridge);
            ingredientRepository.save(entity);
            saved++;
        }
        return saved;
    }

    // 5. 유통기한 임박 식재료 — 특정 냉장고(또는 기본) 기준.
    public List<ExpiringIngredientResponseDto> findExpiring(String username, int days, Long fridgeId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));
        Fridge fridge = resolveFridge(user, fridgeId);

        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(days);

        return ingredientRepository.findByFridge(fridge).stream()
                .filter(ing -> ing.getExpirationDate() != null)
                .filter(ing -> !ing.getExpirationDate().isAfter(threshold))
                .sorted(Comparator.comparing(Ingredient::getExpirationDate))
                .map(ing -> new ExpiringIngredientResponseDto(ing, today))
                .collect(Collectors.toList());
    }

    // 기존 시그니처 호환용
    public List<ExpiringIngredientResponseDto> findExpiring(String username, int days) {
        return findExpiring(username, days, null);
    }

    // 식재료 이벤트(추가/삭제) 시 같은 냉장고의 다른 멤버들에게 푸시.
    // 본인은 제외 — 자기가 한 행동이라 알림이 의미 없음.
    // 멤버가 본인 1명뿐이면 sendToUsers가 빈 리스트로 no-op.
    // route 형식: "ingredient:{id}"이면 클라이언트가 해당 식재료 수정 화면까지 직행, "ingredients"이면 식재료 목록 탭.
    private void notifyOtherMembers(Fridge fridge, User actor, String title, String body, String route) {
        List<User> others = fridgeMemberRepository.findByFridge(fridge).stream()
                .map(FridgeMember::getUser)
                .filter(u -> !u.getUsername().equals(actor.getUsername()))
                .toList();
        appNotificationService.notifyUsers(others, title, body, route);
    }
}
