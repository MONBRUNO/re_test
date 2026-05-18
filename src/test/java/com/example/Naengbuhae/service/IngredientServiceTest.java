package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Category;
import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.Storage;
import com.example.Naengbuhae.dto.ExpiringIngredientResponseDto;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    IngredientRepository ingredientRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    FridgeRepository fridgeRepository;
    @Mock
    FridgeMemberRepository fridgeMemberRepository;

    @InjectMocks
    IngredientService service;

    User user;
    Fridge fridge;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        fridge = mock(Fridge.class);
        lenient().when(user.getAllergies()).thenReturn(null);
        lenient().when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        // 사용자에게 기본 냉장고 1개가 있다고 가정 (Phase A 마이그레이션 후 상태)
        lenient().when(fridgeRepository.findAllForMember(user)).thenReturn(List.of(fridge));
    }

    @Nested
    @DisplayName("saveIngredient")
    class SaveIngredient {

        @Test
        @DisplayName("응답에 allergyWarnings 채워서 반환 (식재료가 알레르기 매칭될 때)")
        void appliesAllergyWarnings() {
            when(user.getAllergies()).thenReturn("땅콩");

            IngredientRequestDto request = new IngredientRequestDto();
            request.setName("땅콩잼");
            request.setQuantity(1.0);
            request.setExpirationDate(LocalDate.now().plusDays(30));
            request.setCategory(Category.ETC);
            request.setUnit("개");
            request.setStorage(Storage.REFRIGERATED);
            request.setPurchaseDate(LocalDate.now());

            Ingredient saved = ingredient("땅콩잼");
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(saved);

            IngredientResponseDto result = service.saveIngredient(request, "alice");

            assertThat(result.getName()).isEqualTo("땅콩잼");
            assertThat(result.getAllergyWarnings()).containsExactly("땅콩");
        }

        @Test
        @DisplayName("알레르기 매칭 안 되는 식재료는 allergyWarnings 비어있음")
        void noAllergyMatch() {
            when(user.getAllergies()).thenReturn("땅콩");

            IngredientRequestDto request = new IngredientRequestDto();
            request.setName("토마토");
            request.setQuantity(1.0);
            request.setExpirationDate(LocalDate.now().plusDays(30));
            request.setCategory(Category.VEGETABLE);
            request.setUnit("개");
            request.setStorage(Storage.REFRIGERATED);
            request.setPurchaseDate(LocalDate.now());

            Ingredient saved = ingredient("토마토");
            when(ingredientRepository.save(any(Ingredient.class))).thenReturn(saved);

            IngredientResponseDto result = service.saveIngredient(request, "alice");

            assertThat(result.getAllergyWarnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllIngredients")
    class FindAllIngredients {

        @Test
        @DisplayName("각 식재료에 알레르기 매칭 키워드 첨부")
        void allergyWarningsPerItem() {
            when(user.getAllergies()).thenReturn("땅콩, 우유");
            when(ingredientRepository.findByFridge(fridge)).thenReturn(List.of(
                    ingredient("땅콩잼"),
                    ingredient("우유"),
                    ingredient("토마토")
            ));

            List<IngredientResponseDto> result = service.findAllIngredients("alice");

            assertThat(result).hasSize(3);
            // 각 항목별 매칭된 키워드만
            assertThat(byName(result, "땅콩잼").getAllergyWarnings()).containsExactly("땅콩");
            assertThat(byName(result, "우유").getAllergyWarnings()).containsExactly("우유");
            assertThat(byName(result, "토마토").getAllergyWarnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findExpiring")
    class FindExpiring {

        @Test
        @DisplayName("days 이내 임박 + 만료된 것 모두 포함, 임박순 정렬")
        void includesExpiredAndUpcoming() {
            LocalDate today = LocalDate.now();
            when(ingredientRepository.findByFridge(fridge)).thenReturn(List.of(
                    ingredient("어제만료", today.minusDays(1)),       // 만료
                    ingredient("오늘만료", today),                     // 오늘
                    ingredient("3일후", today.plusDays(3)),            // 임박
                    ingredient("10일후", today.plusDays(10)),          // 범위 밖
                    ingredient("유통기한없음", null)                    // null → 제외 (filter)
            ));

            List<ExpiringIngredientResponseDto> result = service.findExpiring("alice", 3);

            // 어제만료, 오늘만료, 3일후 — 3개. 10일후/유통기한없음은 제외.
            assertThat(result).hasSize(3);
            // 임박순(오름차순) 정렬
            assertThat(result.get(0).getName()).isEqualTo("어제만료");
            assertThat(result.get(1).getName()).isEqualTo("오늘만료");
            assertThat(result.get(2).getName()).isEqualTo("3일후");
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

    private IngredientResponseDto byName(List<IngredientResponseDto> list, String name) {
        return list.stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
    }
}
