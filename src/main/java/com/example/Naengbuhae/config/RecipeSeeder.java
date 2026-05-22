package com.example.Naengbuhae.config;

import com.example.Naengbuhae.domain.Difficulty;
import com.example.Naengbuhae.domain.Nutrition;
import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.domain.RecipeCategory;
import com.example.Naengbuhae.domain.RecipeIngredient;
import com.example.Naengbuhae.domain.enums.ActivityLevel;
import com.example.Naengbuhae.domain.enums.DietGoal;
import com.example.Naengbuhae.domain.enums.Gender;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.example.Naengbuhae.user.UserRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 부팅 시 resources/seed-recipes.json의 레시피를 시드한다. 소유자는 'system' admin 계정.
// 이름 기준 멱등 — 이미 같은 이름의 레시피가 있으면 건너뛰므로, JSON에 항목을 추가하면
// 다음 배포 때 신규 레시피만 추가된다 ("레시피 더 추가"가 JSON 편집만으로 됨).
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RecipeSeeder {

    private static final String SYSTEM_USERNAME = "system";

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ApplicationRunner seedRecipes() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return args -> tx.executeWithoutResult(status -> {
            User systemUser = userRepository.findByUsername(SYSTEM_USERNAME)
                    .orElseGet(this::createSystemUser);

            // 이미 시드된 system 레시피 이름 — 중복 추가 방지 (이름 기준 멱등)
            Set<String> existingNames = recipeRepository.findByUser(systemUser).stream()
                    .map(Recipe::getName)
                    .collect(Collectors.toSet());

            List<RecipeSeed> seeds = loadSeeds();
            int added = 0;
            for (RecipeSeed s : seeds) {
                if (s.name() == null || existingNames.contains(s.name())) continue;
                Recipe recipe;
                try {
                    recipe = s.toRecipe(systemUser);
                } catch (Exception e) {
                    // JSON 항목 오타(잘못된 category/difficulty 등)로 시드 전체·앱 부팅이
                    // 죽지 않도록 — 해당 항목만 건너뛴다.
                    log.warn("[RecipeSeeder] 레시피 '{}' 변환 실패 — 건너뜀: {}", s.name(), e.getMessage());
                    continue;
                }
                recipeRepository.save(recipe);
                added++;
            }
            log.info("[RecipeSeeder] 레시피 시드 — 신규 {}개 추가 (JSON {}개 / 기존 {}개)",
                    added, seeds.size(), existingNames.size());
        });
    }

    private List<RecipeSeed> loadSeeds() {
        try (InputStream in = new ClassPathResource("seed-recipes.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<RecipeSeed>>() {});
        } catch (Exception e) {
            log.error("[RecipeSeeder] seed-recipes.json 로드 실패 — 시드를 건너뜁니다.", e);
            return List.of();
        }
    }

    private User createSystemUser() {
        // 외부에서 로그인 못하도록 추측 불가능한 랜덤 비번. 권한은 ADMIN.
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = new User(
                SYSTEM_USERNAME,
                randomPassword,
                UserRole.ADMIN,
                "냉부해 시스템",
                Gender.MALE,
                170.0,
                60.0,
                LocalDate.of(2000, 1, 1),
                "system@naengbuhae.local",
                ActivityLevel.MEDIUM,
                DietGoal.MAINTAIN,
                null
        );
        user.setRecommendedCalories(2000);
        userRepository.save(user);
        log.info("[RecipeSeeder] 'system' 시드 사용자 계정을 생성했습니다.");
        return user;
    }

    // === seed-recipes.json 파싱용 DTO (엔티티 아님) ===

    private record RecipeSeed(
            String name,
            String category,
            String difficulty,
            Integer cookingTime,
            Integer servings,
            List<String> steps,
            NutritionSeed nutrition,
            List<IngredientSeed> ingredients) {

        Recipe toRecipe(User owner) {
            Recipe recipe = new Recipe(
                    owner,
                    name,
                    RecipeCategory.valueOf(category),
                    Difficulty.valueOf(difficulty),
                    cookingTime,
                    servings,
                    null,
                    steps,
                    new Nutrition(nutrition.calories(), nutrition.protein(),
                            nutrition.carbs(), nutrition.fat(), nutrition.sodium()));
            for (IngredientSeed i : ingredients) {
                recipe.addIngredient(new RecipeIngredient(
                        recipe, i.name(), i.quantity(), i.unit(), i.required()));
            }
            return recipe;
        }
    }

    private record NutritionSeed(int calories, int protein, int carbs, int fat, int sodium) {}

    private record IngredientSeed(String name, double quantity, String unit, boolean required) {}
}
