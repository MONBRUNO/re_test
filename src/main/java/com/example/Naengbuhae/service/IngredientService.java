package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.ExpiringIngredientResponseDto;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
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

    // 1. 저장 — 응답에 allergyWarnings 채워서 등록 직후 사용자가 알레르기 매칭을 알 수 있게.
    //    (이전엔 Long ID만 반환 → 알레르기 경고를 표시할 수 없었음)
    @Transactional
    public IngredientResponseDto saveIngredient(IngredientRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        Ingredient saved = ingredientRepository.save(requestDto.toEntity(user));
        Set<String> allergens = AllergyMatcher.parseAllergens(user.getAllergies());
        return toResponseWithAllergyWarnings(saved, allergens);
    }

    // 2. 조회 — 특정 사용자의 식재료. 사용자 알레르기와 매칭된 키워드도 함께.
    public List<IngredientResponseDto> findAllIngredients(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        Set<String> allergens = AllergyMatcher.parseAllergens(user.getAllergies());

        return ingredientRepository.findByUser(user).stream()
                .map(ing -> toResponseWithAllergyWarnings(ing, allergens))
                .collect(Collectors.toList());
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

    // 3. 식재료 삭제 기능 (본인 확인 로직 추가)
    @Transactional
    public void deleteIngredient(Long id, String username) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 식재료가 없습니다. id=" + id));
        
        if (!ingredient.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 식재료만 삭제할 수 있습니다.");
        }

        ingredientRepository.delete(ingredient);
    }

    // 4. 식재료 수정 기능 (본인 확인 로직 추가)
    @Transactional
    public Long updateIngredient(Long id, IngredientRequestDto requestDto, String username) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 식재료가 없습니다. id=" + id));

        if (!ingredient.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 식재료만 수정할 수 있습니다.");
        }

        ingredient.setName(requestDto.getName());
        ingredient.setQuantity(requestDto.getQuantity());
        ingredient.setExpirationDate(requestDto.getExpirationDate());
        ingredient.setCategory(requestDto.getCategory());
        ingredient.setUnit(requestDto.getUnit());
        ingredient.setStorage(requestDto.getStorage());
        ingredient.setPurchaseDate(requestDto.getPurchaseDate());

        return ingredient.getId();
    }

    public long countIngredients() {
        return ingredientRepository.count();
    }

    // 5. 유통기한 임박 식재료 조회 (만료된 것 포함, 임박순 정렬)
    //   days 파라미터: N일 이내(만료 포함). 예) days=3 → 만료된 것 + 3일 이내 만료될 것
    public List<ExpiringIngredientResponseDto> findExpiring(String username, int days) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(days);

        return ingredientRepository.findByUser(user).stream()
                .filter(ing -> ing.getExpirationDate() != null)
                // 임계일자 이전 또는 같음 (오늘 만료된 것 + N일 이내 만료될 것 + 이미 만료된 것)
                .filter(ing -> !ing.getExpirationDate().isAfter(threshold))
                .sorted(Comparator.comparing(Ingredient::getExpirationDate))
                .map(ing -> new ExpiringIngredientResponseDto(ing, today))
                .collect(Collectors.toList());
    }
}
