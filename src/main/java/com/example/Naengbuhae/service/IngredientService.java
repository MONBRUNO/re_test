package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    // 1. 저장할 때: 원본 대신 '받는 택배 상자(RequestDto)'를 받음
    @Transactional
    public Long saveIngredient(IngredientRequestDto requestDto) {
        // 상자 내용물을 원본(Entity)으로 뜯어서 변환한 다음, DB 창고에 저장!
        return ingredientRepository.save(requestDto.toEntity()).getId();
    }

    // 2. 조회할 때: 원본 대신 '보내는 택배 상자(ResponseDto)' 리스트를 뱉음
    public List<IngredientResponseDto> findAllIngredients() {
        // DB 창고에서 원본들을 싹 꺼내온 다음, 하나하나 예쁜 택배 상자(DTO)에 옮겨 담아서(map) 반환!
        return ingredientRepository.findAll().stream()
                .map(IngredientResponseDto::new) // Ingredient 원본을 ResponseDto로 포장하는 마법의 코드
                .collect(Collectors.toList());
    }

    // --- 3. 식재료 삭제 기능 ---
    @Transactional
    public void deleteIngredient(Long id) {
        // 창고지기한테 "이 번호표(id) 가진 식재료 찾아서 버려!" 라고 시킴
        ingredientRepository.deleteById(id);
    }

    // --- 4. 식재료 수정 기능 (Update) ---
    // @Transactional이 여기서 진짜 중요한 마법을 부림!
    @Transactional
    public Long updateIngredient(Long id, IngredientRequestDto requestDto) {
        // 1. 창고에서 수정할 식재료를 번호(id)로 찾아온다. (없으면 에러 뱉음!)
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 식재료가 없습니다. id=" + id));

        // 2. 찾아온 원본 식재료의 정보를 새 택배 상자(DTO)에 담긴 정보로 바꿔치기!
        ingredient.setName(requestDto.getName());
        ingredient.setQuantity(requestDto.getQuantity());
        ingredient.setExpirationDate(requestDto.getExpirationDate());

        // 3. 엥? 저장(save)을 안 하네?!
        // 👉 맞음! 스프링 JPA의 '변경 감지' 마법 덕분에 값만 바꿔도 알아서 DB에 덮어씌워짐!
        return ingredient.getId();
    }
}