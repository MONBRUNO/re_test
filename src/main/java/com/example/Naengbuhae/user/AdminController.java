package com.example.Naengbuhae.user;

import com.example.Naengbuhae.config.Audit; // ✨ 우리가 만든 마법의 어노테이션
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.dto.SystemStatsResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import com.example.Naengbuhae.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ✨ AOP 적용 완료! 컨트롤러는 오직 '삭제'에만 집중함.
    @DeleteMapping("/recipes/{recipeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(action = "DELETE_RECIPE", targetType = "RECIPE", idParamName = "recipeId", description = "관리자 권한으로 레시피 강제 삭제 (부적절한 콘텐츠)")
    public String deleteRecipeByAdmin(@PathVariable Long recipeId) {
        
        recipeService.deleteRecipeByAdmin(recipeId);
        
        return recipeId + "번 레시피가 삭제 처리되었습니다.";
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

    @PutMapping("/users/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    // 😎 우리가 만든 마법의 감사 로그 어노테이션 발동!
    @Audit(action = "BAN_USER", targetType = "USER", idParamName = "userId", description = "관리자 권한으로 특정 유저 계정 정지 혹은 정지 해제")
    public ApiResponse toggleUserBan(
            @PathVariable Long userId, 
            @RequestParam boolean ban) {
        
        userService.updateUserBanStatus(userId, ban);
        
        String message = ban ? "해당 사용자가 정지 처리되었습니다." : "해당 사용자의 정지가 해제되었습니다.";
        return new ApiResponse(true, message);
    }
}