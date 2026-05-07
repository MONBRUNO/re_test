package com.example.Naengbuhae.config;

import com.example.Naengbuhae.domain.Difficulty;
import com.example.Naengbuhae.domain.Nutrition;
import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.domain.RecipeCategory;
import com.example.Naengbuhae.domain.RecipeIngredient;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.example.Naengbuhae.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 부팅 시 recipe 테이블이 비어있으면 프론트의 8개 하드코딩 레시피를 자동 시드.
// 소유자는 'system' admin 계정 (없으면 자동 생성).
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RecipeSeeder {

    private static final String SYSTEM_USERNAME = "system";

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public ApplicationRunner seedRecipes() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return args -> tx.executeWithoutResult(status -> {
            if (recipeRepository.count() > 0) {
                log.info("[RecipeSeeder] recipe 테이블이 비어있지 않아 시드를 건너뜁니다.");
                return;
            }

            User systemUser = userRepository.findByUsername(SYSTEM_USERNAME)
                    .orElseGet(this::createSystemUser);

            List<Recipe> recipes = buildRecipes(systemUser);
            recipeRepository.saveAll(recipes);
            log.info("[RecipeSeeder] {}개의 기본 레시피를 시드했습니다.", recipes.size());
        });
    }

    private User createSystemUser() {
        // 외부에서 로그인 못하도록 추측 불가능한 랜덤 비번. 권한은 ADMIN.
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = new User(
                SYSTEM_USERNAME,
                randomPassword,
                UserRole.ADMIN,
                "냉부해 시스템",
                "남",
                170.0,
                60.0,
                LocalDate.of(2000, 1, 1),
                "system@naengbuhae.local",
                "보통 활동",
                "체중 유지",
                null
        );
        user.setRecommendedCalories(2000);
        userRepository.save(user);
        log.info("[RecipeSeeder] 'system' 시드 사용자 계정을 생성했습니다.");
        return user;
    }

    private List<Recipe> buildRecipes(User owner) {
        return List.of(
                recipe(owner, "계란볶음밥", RecipeCategory.MAIN, Difficulty.easy, 15, 2,
                        List.of(
                                "계란을 풀어서 소금으로 간을 합니다.",
                                "팬에 기름을 두르고 계란을 먼저 볶아줍니다.",
                                "야채를 잘게 썰어 함께 볶습니다.",
                                "밥을 넣고 고루 섞으면서 볶습니다.",
                                "간장이나 소금으로 간을 맞추고 완성합니다."
                        ),
                        new Nutrition(520, 18, 72, 16, 680),
                        List.of(
                                ing("계란", 2.0, "개", true),
                                ing("밥", 2.0, "공기", true),
                                ing("양파", 0.5, "개", false),
                                ing("당근", 0.3, "개", false),
                                ing("파", 1.0, "줌", false)
                        )),
                recipe(owner, "닭가슴살 샐러드", RecipeCategory.SALAD, Difficulty.easy, 20, 2,
                        List.of(
                                "닭가슴살을 삶아서 결대로 찢어줍니다.",
                                "양상추를 한입 크기로 뜯어줍니다.",
                                "토마토와 오이를 먹기 좋은 크기로 썰어줍니다.",
                                "모든 재료를 그릇에 담고 드레싱을 뿌려 완성합니다."
                        ),
                        new Nutrition(180, 28, 8, 4, 320),
                        List.of(
                                ing("닭가슴살", 200.0, "g", true),
                                ing("양상추", 1.0, "개", true),
                                ing("토마토", 1.0, "개", false),
                                ing("오이", 0.5, "개", false)
                        )),
                recipe(owner, "우유 스크램블 에그", RecipeCategory.SNACK, Difficulty.easy, 10, 1,
                        List.of(
                                "계란과 우유를 잘 섞어줍니다.",
                                "팬에 버터를 녹이고 약불로 조절합니다.",
                                "계란 혼합물을 넣고 천천히 저어가며 익힙니다.",
                                "치즈를 올려 완성합니다."
                        ),
                        new Nutrition(280, 21, 4, 20, 420),
                        List.of(
                                ing("계란", 3.0, "개", true),
                                ing("우유", 50.0, "ml", true),
                                ing("치즈", 1.0, "장", false)
                        )),
                recipe(owner, "닭가슴살 볶음", RecipeCategory.SIDE, Difficulty.easy, 15, 2,
                        List.of(
                                "닭가슴살을 한입 크기로 잘라줍니다.",
                                "야채를 먹기 좋은 크기로 썰어줍니다.",
                                "팬에 기름을 두르고 닭가슴살을 먼저 볶습니다.",
                                "야채를 넣고 함께 볶아줍니다.",
                                "간장, 올리고당으로 간을 맞춰 완성합니다."
                        ),
                        new Nutrition(240, 38, 12, 5, 580),
                        List.of(
                                ing("닭가슴살", 300.0, "g", true),
                                ing("양파", 1.0, "개", false),
                                ing("브로콜리", 0.5, "개", false),
                                ing("파프리카", 0.5, "개", false)
                        )),
                recipe(owner, "당근 계란찜", RecipeCategory.SIDE, Difficulty.easy, 25, 2,
                        List.of(
                                "계란을 풀어서 체에 거릅니다.",
                                "당근을 잘게 다져서 넣습니다.",
                                "물을 계란과 1:1 비율로 넣고 섞습니다.",
                                "찜기에 넣고 약불로 20분간 찝니다.",
                                "파를 올려 완성합니다."
                        ),
                        new Nutrition(160, 14, 6, 10, 280),
                        List.of(
                                ing("계란", 4.0, "개", true),
                                ing("당근", 0.5, "개", true),
                                ing("파", 1.0, "줌", false)
                        )),
                recipe(owner, "양상추 샌드위치", RecipeCategory.SNACK, Difficulty.easy, 10, 1,
                        List.of(
                                "식빵을 가볍게 구워줍니다.",
                                "계란을 프라이로 익힙니다.",
                                "토마토를 슬라이스합니다.",
                                "식빵에 재료를 차례로 올려 완성합니다."
                        ),
                        new Nutrition(320, 15, 38, 12, 520),
                        List.of(
                                ing("식빵", 2.0, "장", true),
                                ing("양상추", 2.0, "장", true),
                                ing("토마토", 0.5, "개", false),
                                ing("치즈", 1.0, "장", false),
                                ing("계란", 1.0, "개", false)
                        )),
                recipe(owner, "우유 요거트 스무디", RecipeCategory.DRINK, Difficulty.easy, 5, 1,
                        List.of(
                                "모든 재료를 믹서기에 넣습니다.",
                                "부드럽게 갈아줍니다.",
                                "컵에 담아 완성합니다."
                        ),
                        new Nutrition(220, 12, 36, 4, 150),
                        List.of(
                                ing("우유", 200.0, "ml", true),
                                ing("요거트", 100.0, "g", true),
                                ing("바나나", 1.0, "개", false),
                                ing("딸기", 5.0, "개", false)
                        )),
                recipe(owner, "소고기 야채볶음", RecipeCategory.SIDE, Difficulty.medium, 20, 2,
                        List.of(
                                "소고기를 간장, 설탕, 마늘로 양념합니다.",
                                "야채를 먹기 좋은 크기로 썰어줍니다.",
                                "팬에 소고기를 먼저 볶습니다.",
                                "야채를 넣고 함께 볶아 완성합니다."
                        ),
                        new Nutrition(280, 26, 14, 14, 720),
                        List.of(
                                ing("소고기", 200.0, "g", true),
                                ing("양파", 1.0, "개", true),
                                ing("당근", 0.5, "개", false),
                                ing("브로콜리", 0.5, "개", false)
                        ))
        );
    }

    private static Recipe recipe(User user, String name, RecipeCategory category, Difficulty difficulty,
                                 int cookingTime, int servings, List<String> steps,
                                 Nutrition nutrition, List<IngredientSpec> specs) {
        Recipe recipe = new Recipe(user, name, category, difficulty, cookingTime, servings, null, steps, nutrition);
        for (IngredientSpec s : specs) {
            recipe.addIngredient(new RecipeIngredient(recipe, s.name, s.quantity, s.unit, s.required));
        }
        return recipe;
    }

    private static IngredientSpec ing(String name, Double quantity, String unit, boolean required) {
        return new IngredientSpec(name, quantity, unit, required);
    }

    private record IngredientSpec(String name, Double quantity, String unit, boolean required) {}
}
