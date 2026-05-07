package com.example.Naengbuhae.user;

import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.dto.SystemStatsResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import com.example.Naengbuhae.service.RecipeService;
import com.example.Naengbuhae.service.AuditLogService; // ✨ 감사 로그 서비스 import
import jakarta.servlet.http.HttpServletRequest; // ✨ 클라이언트 IP 추출용 import
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
    private final AuditLogService auditLogService; // ✨ 감사 로그 서비스 의존성 주입

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

    // ✨ 삭제 API에 HttpServletRequest를 추가해서 접속 IP를 가져오도록 수정
    @DeleteMapping("/recipes/{recipeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRecipeByAdmin(@PathVariable Long recipeId, HttpServletRequest request) {
        // [Audit] 현재 삭제 행위를 수행하는 관리자 이름 추출
        String adminName = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. 실제 삭제 로직 수행
        recipeService.deleteRecipeByAdmin(recipeId);

        // ✨ 2. DB에 감사 로그 저장!
        auditLogService.logAction(
                adminName,
                "DELETE_RECIPE",
                "RECIPE",
                recipeId,
                "관리자에 의한 강제 삭제 조치 (부적절한 콘텐츠)",
                request.getRemoteAddr() // 접속 IP 추출
        );

        // 3. 기존의 서버 콘솔 로깅도 유지
        log.warn("[Audit] 관리자 '{}'가 레시피 ID '{}'번을 강제 삭제 조치했습니다.", adminName, recipeId);

        return recipeId + "번 레시피가 관리자에 의해 강제 삭제되었으며, DB에 감사 로그가 기록되었습니다.";
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