package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.domain.RecipeIngredient;
import com.example.Naengbuhae.dto.RecipeIngredientDto;
import com.example.Naengbuhae.dto.RecipeMatchResponseDto;
import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.repository.RecipeFavoriteRepository;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.example.Naengbuhae.util.AllergyMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeFavoriteRepository recipeFavoriteRepository;
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository; // ✨ 의존성 추가 완료

    // 1. 레시피 저장 (새 필드 + 재료 목록)
    @Transactional
    public Long saveRecipe(RecipeRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        Recipe recipe = requestDto.toEntity(user);
        applyIngredients(recipe, requestDto.getIngredients());

        return recipeRepository.save(recipe).getId();
    }

    // 2. 내 레시피 전체 조회 — 사용자의 알레르기 정보로 각 레시피에 경고 첨부 + 즐겨찾기 여부 표시
    public List<RecipeResponseDto> findAllRecipes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        Set<String> allergens = AllergyMatcher.parseAllergens(user.getAllergies());
        Set<Long> favoriteIds = new HashSet<>(recipeFavoriteRepository.findRecipeIdsByUser(user));

        return recipeRepository.findByUser(user).stream()
                .map(recipe -> toResponseWithMeta(recipe, allergens, favoriteIds))
                .toList();
    }

    // 8. 즐겨찾기 토글 — 이미 표시되어 있으면 제거, 없으면 추가. 새 상태(true=즐겨찾기) 반환.
    @Transactional
    public boolean toggleFavorite(Long recipeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다."));
        if (recipeFavoriteRepository.existsByUserAndRecipe(user, recipe)) {
            recipeFavoriteRepository.deleteByUserAndRecipe(user, recipe);
            return false;
        }
        recipeFavoriteRepository.save(new com.example.Naengbuhae.domain.RecipeFavorite(user, recipe));
        return true;
    }

    // 3. 레시피 수정 (권한 체크 + 모든 필드 갱신)
    @Transactional
    public Long updateRecipe(Long id, RecipeRequestDto requestDto, String username) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다. id=" + id));

        if (!recipe.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 레시피만 수정할 수 있습니다.");
        }

        recipe.setName(requestDto.getName());
        recipe.setCategory(requestDto.getCategory());
        recipe.setDifficulty(requestDto.getDifficulty());
        recipe.setCookingTime(requestDto.getCookingTime());
        recipe.setServings(requestDto.getServings());
        recipe.setImageUrl(requestDto.getImageUrl());
        recipe.setSteps(requestDto.getSteps() != null ? new ArrayList<>(requestDto.getSteps()) : new ArrayList<>());
        recipe.setNutrition(requestDto.getNutrition() != null ? requestDto.getNutrition().toEntity() : null);

        // 재료 목록 통째 갱신: orphanRemoval이 자동 삭제
        recipe.getIngredients().clear();
        applyIngredients(recipe, requestDto.getIngredients());

        return recipe.getId();
    }

    // 4. 레시피 삭제
    @Transactional
    public void deleteRecipe(Long id, String username) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다. id=" + id));

        if (!recipe.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 레시피만 삭제할 수 있습니다.");
        }

        recipeFavoriteRepository.deleteByRecipe(recipe);
        recipeRepository.delete(recipe);
    }

    // 5. 관리자용 레시피 강제 삭제
    @Transactional
    public void deleteRecipeByAdmin(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다. id=" + id));
        recipeFavoriteRepository.deleteByRecipe(recipe);
        recipeRepository.delete(recipe);
    }

    // 6. 관리자용 전체 레시피 조회
    public List<RecipeResponseDto> getAllRecipesByAdmin() {
        return recipeRepository.findAllWithUser().stream()
                .map(RecipeResponseDto::new)
                .collect(Collectors.toList());
    }

    public long countRecipes() {
        return recipeRepository.count();
    }

    /**
     * 💡 기존의 '내 식재료만' 보던 로직에서 -> '우리 가족 공유 냉장고' 로직으로 대규모 업그레이드!
     */
    public List<RecipeMatchResponseDto> recommendRecipes(String username, Long fridgeId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        // 1. 대상 냉장고 결정 (fridgeId가 없으면 사용자의 첫 번째 냉장고 사용)
        Fridge fridge;
        if (fridgeId != null) {
            fridge = fridgeRepository.findById(fridgeId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 냉장고를 찾을 수 없습니다."));
            
            // ✨ 핵심 보안 패치: 이 유저가 이 냉장고의 멤버가 맞는지 확인! (IDOR 방어)
            if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, user)) {
                log.warn("🚨 [보안 경고] 유저 '{}'가 권한 없는 냉장고 '{}'에 접근 시도!", username, fridgeId);
                throw new IllegalArgumentException("해당 냉장고에 접근할 권한이 없습니다.");
            }
        } else {
            List<Fridge> myFridges = fridgeRepository.findAllForMember(user);
            if (myFridges.isEmpty()) {
                return List.of();
            }
            fridge = myFridges.get(0);
        }

        LocalDate today = LocalDate.now();
        // 2. ✨ 핵심! 본인뿐만 아니라 냉장고를 공유하는 모든 가족의 식재료를 가져옴
        Set<String> familyIngredientNames = ingredientRepository.findByFridge(fridge).stream()
                .filter(ing -> ing.getExpirationDate() == null || !ing.getExpirationDate().isBefore(today))
                .map(Ingredient::getName)
                .map(this::normalizeName)
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> allergens = AllergyMatcher.parseAllergens(user.getAllergies());
        Set<Long> favoriteIds = new HashSet<>(recipeFavoriteRepository.findRecipeIdsByUser(user));

        return recipeRepository.findAllWithUserAndIngredients().stream()
                .map(recipe -> buildMatch(recipe, familyIngredientNames, allergens, favoriteIds))
                .filter(match -> match.getRecipe().getAllergyWarnings().isEmpty()) // 알레르기 매칭 레시피 제외
                .sorted(Comparator.comparingInt(RecipeMatchResponseDto::getMatchRate).reversed())
                .collect(Collectors.toList());
    }

    // 기존 시그니처 유지 (호환성)
    public List<RecipeMatchResponseDto> recommendRecipes(String username) {
        return recommendRecipes(username, null);
    }

    // ===== 내부 헬퍼 =====

    private void applyIngredients(Recipe recipe, List<RecipeIngredientDto> ingredientDtos) {
        if (ingredientDtos == null) return;
        for (RecipeIngredientDto dto : ingredientDtos) {
            recipe.addIngredient(new RecipeIngredient(
                    recipe, dto.getName(), dto.getQuantity(), dto.getUnit(), dto.isRequired()
            ));
        }
    }

    private RecipeMatchResponseDto buildMatch(Recipe recipe, Set<String> myIngredientNames,
                                              Set<String> allergens, Set<Long> favoriteIds) {
        List<String> has = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        boolean hasAllRequired = true;

        for (RecipeIngredient ri : recipe.getIngredients()) {
            if (containsByPartialMatch(myIngredientNames, ri.getName())) {
                has.add(ri.getName());
            } else {
                missing.add(ri.getName());
                if (ri.isRequired()) hasAllRequired = false;
            }
        }

        int total = recipe.getIngredients().size();
        int matchRate = 0;
        if (hasAllRequired && total > 0) {
            matchRate = (int) Math.round((has.size() * 100.0) / total);
        }

        RecipeResponseDto recipeDto = toResponseWithMeta(recipe, allergens, favoriteIds);
        return new RecipeMatchResponseDto(recipeDto, matchRate, has, missing);
    }

    // 레시피 → DTO 변환 + 알레르기 키워드 매칭 + 즐겨찾기 여부 채움
    private RecipeResponseDto toResponseWithMeta(Recipe recipe, Set<String> allergens, Set<Long> favoriteIds) {
        RecipeResponseDto dto = new RecipeResponseDto(recipe);
        if (!allergens.isEmpty()) {
            List<String> ingredientNames = recipe.getIngredients().stream()
                    .map(RecipeIngredient::getName)
                    .toList();
            dto.setAllergyWarnings(new ArrayList<>(AllergyMatcher.findMatches(allergens, ingredientNames)));
        }
        dto.setFavorite(favoriteIds.contains(recipe.getId()));
        return dto;
    }

    // 프론트와 동일한 부분 매칭 (서로 includes — 양쪽 어느 쪽이든 포함하면 매치)
    private boolean containsByPartialMatch(Set<String> myNames, String recipeIngName) {
        String target = normalizeName(recipeIngName);
        for (String mine : myNames) {
            if (mine.contains(target) || target.contains(mine)) return true;
        }
        return false;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
