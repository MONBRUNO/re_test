package com.example.Naengbuhae.config;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.example.Naengbuhae.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 앱 시작 시 한 번 실행: 냉장고를 한 번도 안 가진 사용자에게 "내 냉장고" 자동 생성하고
// 기존 식재료(fridge=null)를 그 냉장고로 옮긴다.
//
// 이미 냉장고가 있는 사용자는 건너뜀 → 재실행 안전.
// Order(2)는 RecipeSeeder 등 다른 startup 작업과 충돌 안 나게 살짝 늦게 실행.
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class FridgeMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final IngredientRepository ingredientRepository;

    private static final String LEGACY_DEFAULT_NAME = "내 냉장고";

    @Override
    @Transactional
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        int created = 0;
        int renamed = 0;
        int movedIngredients = 0;

        for (User user : users) {
            // 이미 멤버로 속한 냉장고가 있으면 건너뜀 (재실행 안전)
            boolean hasAny = !fridgeMemberRepository.findByUser(user).isEmpty();
            if (hasAny) {
                // 옛 이름("내 냉장고") 일괄 리네임: 본인이 만들었고 정확히 옛 이름이면 새 이름으로.
                // 사용자가 의도적으로 그 이름을 쓴 경우는 거의 없을 것으로 가정.
                for (Fridge owned : fridgeRepository.findByOwner(user)) {
                    if (LEGACY_DEFAULT_NAME.equals(owned.getName())) {
                        owned.setName(UserService.defaultFridgeName(user));
                        renamed++;
                    }
                }
                // 그래도 fridge=null 식재료가 있으면 첫 냉장고로 옮긴다 (혹시 모를 누락 보정)
                Fridge first = fridgeRepository.findAllForMember(user).stream().findFirst().orElse(null);
                if (first != null) {
                    movedIngredients += attachOrphanIngredients(user, first);
                }
                continue;
            }

            Fridge fridge = new Fridge(user, UserService.defaultFridgeName(user));
            fridgeRepository.save(fridge);
            fridgeMemberRepository.save(new FridgeMember(fridge, user));
            created++;

            movedIngredients += attachOrphanIngredients(user, fridge);
        }

        if (created > 0 || renamed > 0 || movedIngredients > 0) {
            log.info("[FridgeMigration] 신규 냉장고 {}개 생성, {}개 이름 변경, 식재료 {}개 이관",
                    created, renamed, movedIngredients);
        }
    }

    private int attachOrphanIngredients(User user, Fridge fridge) {
        List<Ingredient> orphans = ingredientRepository.findByUserAndFridgeIsNull(user);
        for (Ingredient i : orphans) {
            i.setFridge(fridge);
        }
        return orphans.size();
    }
}
