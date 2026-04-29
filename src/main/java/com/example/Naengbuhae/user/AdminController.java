package com.example.Naengbuhae.user;

import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RecipeService recipeService;

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
        recipeService.deleteRecipeByAdmin(recipeId);
        return recipeId + "번 레시피가 관리자에 의해 강제 삭제되었습니다. (부적절한 콘텐츠)";
    }
}
