package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.ExpiringIngredientResponseDto;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;

    // 1. 저장할 때: 현재 로그인한 사용자의 정보를 받아와서 연결!
    @Transactional
    public Long saveIngredient(IngredientRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));
        
        return ingredientRepository.save(requestDto.toEntity(user)).getId();
    }

    // 2. 조회할 때: 특정 사용자의 식재료만 필터링해서 조회!
    public List<IngredientResponseDto> findAllIngredients(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));

        return ingredientRepository.findByUser(user).stream()
                .map(IngredientResponseDto::new)
                .collect(Collectors.toList());
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
