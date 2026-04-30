package com.example.Naengbuhae.user;

import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.dto.SystemStatsResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import com.example.Naengbuhae.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RecipeService recipeService;
    private final IngredientService ingredientService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/recipes")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RecipeResponseDto> getAllRecipes() {
        return recipeService.getAllRecipesByAdmin();
    }

    @DeleteMapping("/recipes/{recipeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRecipeByAdmin(@PathVariable Long recipeId) {
        // [Audit] 현재 삭제 행위를 수행하는 관리자 이름 추출
        String adminName = SecurityContextHolder.getContext().getAuthentication().getName();

        recipeService.deleteRecipeByAdmin(recipeId);

        // [Audit Log] 관리자 행위 로깅
        log.warn("[Audit] 관리자 '{}'가 레시피 ID '{}'번을 강제 삭제 조치했습니다.", adminName, recipeId);

        return recipeId + "번 레시피가 관리자에 의해 강제 삭제되었습니다. (부적절한 콘텐츠)";
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemStatsResponseDto getSystemStats() {
        return new SystemStatsResponseDto(
                userService.countUsers(),
                recipeService.countRecipes(),
                ingredientService.countIngredients()
        );
    }
}