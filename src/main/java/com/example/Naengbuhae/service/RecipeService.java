package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    // 1. 레시피 저장 (사용자 연결)
    @Transactional
    public Long saveRecipe(RecipeRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));
        return recipeRepository.save(requestDto.toEntity(user)).getId();
    }

    // 2. 레시피 전체 조회 (로그인한 유저의 것만)
    public List<RecipeResponseDto> findAllRecipes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. username=" + username));
        
        return recipeRepository.findByUser(user).stream()
                .map(RecipeResponseDto::new)
                .collect(Collectors.toList());
    }

    // 3. 레시피 수정 (권한 체크 포함)
    @Transactional
    public Long updateRecipe(Long id, RecipeRequestDto requestDto, String username) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다. id=" + id));

        // 주인이 맞는지 확인!
        if (!recipe.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 레시피만 수정할 수 있습니다.");
        }

        recipe.setTitle(requestDto.getTitle());
        recipe.setInstructions(requestDto.getInstructions());
        recipe.setCookingTime(requestDto.getCookingTime());

        return recipe.getId();
    }

    // 4. 레시피 삭제 (권한 체크 포함)
    @Transactional
    public void deleteRecipe(Long id, String username) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다. id=" + id));

        // 주인이 맞는지 확인!
        if (!recipe.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 레시피만 삭제할 수 있습니다.");
        }

        recipeRepository.delete(recipe);
    }

    // 5. 관리자용 레시피 강제 삭제 (권한 체크 없음 - 컨트롤러에서 ADMIN 체크함)
    @Transactional
    public void deleteRecipeByAdmin(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 레시피가 없습니다. id=" + id));
        recipeRepository.delete(recipe);
    }

    // 6. 관리자용 전체 레시피 조회
    public List<RecipeResponseDto> getAllRecipesByAdmin() {
        return recipeRepository.findAll().stream()
                .map(RecipeResponseDto::new)
                .collect(Collectors.toList());
    }
}
