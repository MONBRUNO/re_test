package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.domain.RecipeIngredient;
import com.example.Naengbuhae.dto.RecipeIngredientDto;
import com.example.Naengbuhae.dto.RecipeMatchResponseDto;
import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.repository.RecipeFavoriteRepository;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.example.Naengbuhae.util.AllergyMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeFavoriteRepository recipeFavoriteRepository;

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

    // 7. 추천 (매칭률 기반) — 프론트의 matchRecipesWithIngredients와 동일한 로직
    //   - 매칭률 = 보유 재료 수 / 전체 재료 수 * 100
    //   - 단, 필수재료(required=true)가 하나라도 빠지면 매칭률 0
    //   - 만료된 재료는 보유로 인정하지 않음
    //   - 사용자 알레르기에 걸리는 레시피는 추천에서 제외 (안전 우선)
    //   - 매칭률 내림차순 정렬
    public List<RecipeMatchResponseDto> recommendRecipes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        LocalDate today = LocalDate.now();
        Set<String> myIngredientNames = ingredientRepository.findByUser(user).stream()
                .filter(ing -> ing.getExpirationDate() == null || !ing.getExpirationDate().isBefore(today))
                .map(Ingredient::getName)
                .map(this::normalizeName)
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> allergens = AllergyMatcher.parseAllergens(user.getAllergies());
        Set<Long> favoriteIds = new HashSet<>(recipeFavoriteRepository.findRecipeIdsByUser(user));

        return recipeRepository.findAllWithUserAndIngredients().stream()
                .map(recipe -> buildMatch(recipe, myIngredientNames, allergens, favoriteIds))
                .filter(match -> match.getRecipe().getAllergyWarnings().isEmpty()) // 알레르기 매칭 레시피 제외
                .sorted(Comparator.comparingInt(RecipeMatchResponseDto::getMatchRate).reversed())
                .collect(Collectors.toList());
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
