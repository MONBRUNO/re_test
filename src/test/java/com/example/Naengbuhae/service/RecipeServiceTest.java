package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Category;
import com.example.Naengbuhae.domain.Difficulty;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.domain.RecipeCategory;
import com.example.Naengbuhae.domain.RecipeIngredient;
import com.example.Naengbuhae.domain.Storage;
import com.example.Naengbuhae.dto.RecipeMatchResponseDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// RecipeService.recommendRecipes는 매칭률 + 알레르기 필터 + 만료 필터 조합이라
// 회귀 위험이 큼. 각 분기를 명시적으로 검증.
@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    RecipeRepository recipeRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    IngredientRepository ingredientRepository;

    @InjectMocks
    RecipeService service;

    User user;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        // 기본은 알레르기 없음 — 필요한 테스트에서 override
        lenient().when(user.getAllergies()).thenReturn(null);
        lenient().when(user.getUsername()).thenReturn("alice");
        lenient().when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    @Nested
    @DisplayName("recommendRecipes")
    class RecommendRecipes {

        @Test
        @DisplayName("사용자 없음 → IllegalArgumentException")
        void userNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recommendRecipes("ghost"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자가 없습니다");
        }

        @Test
        @DisplayName("필수 재료를 모두 보유 → matchRate 100, 매칭률 desc 정렬")
        void allRequiredOwned() {
            Ingredient egg = ingredient("계란");
            Ingredient milk = ingredient("우유");
            when(ingredientRepository.findByUser(user)).thenReturn(List.of(egg, milk));

            Recipe r1 = recipe("계란우유", List.of(reqIng("계란"), reqIng("우유")));
            Recipe r2 = recipe("계란만", List.of(reqIng("계란"), optIng("토마토")));
            when(recipeRepository.findAllWithUserAndIngredients()).thenReturn(List.of(r2, r1));

            List<RecipeMatchResponseDto> result = service.recommendRecipes("alice");

            // matchRate 정렬: 계란우유(100%) > 계란만(50% — 필수 1개만 충족, 토마토 없음)
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRecipe().getName()).isEqualTo("계란우유");
            assertThat(result.get(0).getMatchRate()).isEqualTo(100);
            assertThat(result.get(1).getRecipe().getName()).isEqualTo("계란만");
            assertThat(result.get(1).getMatchRate()).isEqualTo(50);
        }

        @Test
        @DisplayName("필수 재료 누락 → matchRate 0")
        void requiredMissing() {
            Ingredient egg = ingredient("계란");
            when(ingredientRepository.findByUser(user)).thenReturn(List.of(egg));

            Recipe recipe = recipe("필수누락", List.of(reqIng("계란"), reqIng("우유")));
            when(recipeRepository.findAllWithUserAndIngredients()).thenReturn(List.of(recipe));

            List<RecipeMatchResponseDto> result = service.recommendRecipes("alice");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMatchRate()).isZero(); // 필수 우유 없음
        }

        @Test
        @DisplayName("만료된 식재료는 보유 인정 안 됨 → matchRate 영향")
        void expiredIngredientNotCounted() {
            Ingredient freshEgg = ingredient("계란", LocalDate.now().plusDays(3));
            Ingredient expiredMilk = ingredient("우유", LocalDate.now().minusDays(1));
            when(ingredientRepository.findByUser(user)).thenReturn(List.of(freshEgg, expiredMilk));

            Recipe recipe = recipe("계란우유", List.of(reqIng("계란"), reqIng("우유")));
            when(recipeRepository.findAllWithUserAndIngredients()).thenReturn(List.of(recipe));

            List<RecipeMatchResponseDto> result = service.recommendRecipes("alice");

            // 만료 우유는 보유 인정 안 돼서 필수 재료 누락 → matchRate 0
            assertThat(result.get(0).getMatchRate()).isZero();
            assertThat(result.get(0).getMissingIngredients()).contains("우유");
        }

        @Test
        @DisplayName("알레르기 매칭 레시피는 결과에서 제외")
        void allergyRecipesFilteredOut() {
            when(user.getAllergies()).thenReturn("땅콩");
            when(ingredientRepository.findByUser(user)).thenReturn(Collections.emptyList());

            Recipe peanutRecipe = recipe("땅콩잼토스트", List.of(reqIng("식빵"), reqIng("땅콩잼")));
            Recipe safeRecipe = recipe("계란말이", List.of(reqIng("계란")));
            when(recipeRepository.findAllWithUserAndIngredients())
                    .thenReturn(List.of(peanutRecipe, safeRecipe));

            List<RecipeMatchResponseDto> result = service.recommendRecipes("alice");

            // 땅콩잼토스트는 "땅콩잼" 재료가 사용자 알레르기 "땅콩"과 매칭되어 제외됨
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecipe().getName()).isEqualTo("계란말이");
        }
    }

    @Nested
    @DisplayName("findAllRecipes")
    class FindAllRecipes {

        @Test
        @DisplayName("사용자 없음 → IllegalArgumentException")
        void userNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findAllRecipes("ghost"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("내 레시피만 반환 + 알레르기 매칭된 키워드 첨부")
        void appliesAllergyWarnings() {
            when(user.getAllergies()).thenReturn("땅콩");

            Recipe peanutRecipe = recipe("땅콩잼토스트", List.of(reqIng("식빵"), reqIng("땅콩잼")));
            Recipe safeRecipe = recipe("계란말이", List.of(reqIng("계란")));
            when(recipeRepository.findByUser(user)).thenReturn(List.of(peanutRecipe, safeRecipe));

            List<RecipeResponseDto> result = service.findAllRecipes("alice");

            assertThat(result).hasSize(2);
            // 땅콩잼 들어간 레시피는 allergyWarnings에 "땅콩" 포함
            RecipeResponseDto peanut = result.stream()
                    .filter(r -> r.getName().equals("땅콩잼토스트")).findFirst().orElseThrow();
            assertThat(peanut.getAllergyWarnings()).containsExactly("땅콩");

            // 계란말이는 비어있어야 (필터 아님 — 모든 레시피 반환되지만 안전한 건 빈 배열)
            RecipeResponseDto safe = result.stream()
                    .filter(r -> r.getName().equals("계란말이")).findFirst().orElseThrow();
            assertThat(safe.getAllergyWarnings()).isEmpty();
        }
    }

    // ===== 헬퍼 =====

    private Ingredient ingredient(String name) {
        return ingredient(name, LocalDate.now().plusDays(7));
    }

    private Ingredient ingredient(String name, LocalDate expirationDate) {
        return new Ingredient(user, name, 1.0, expirationDate,
                Category.ETC, "개", Storage.REFRIGERATED, LocalDate.now());
    }

    private Recipe recipe(String name, List<RecipeIngredientSpec> ingredients) {
        Recipe r = new Recipe(user, name, RecipeCategory.MAIN, Difficulty.easy,
                10, 1, null, List.of(), null);
        for (RecipeIngredientSpec spec : ingredients) {
            r.addIngredient(new RecipeIngredient(r, spec.name, 1.0, "개", spec.required));
        }
        return r;
    }

    private RecipeIngredientSpec reqIng(String name) {
        return new RecipeIngredientSpec(name, true);
    }

    private RecipeIngredientSpec optIng(String name) {
        return new RecipeIngredientSpec(name, false);
    }

    private record RecipeIngredientSpec(String name, boolean required) {}
}
