package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngredientExpirationScheduler {

    private final IngredientRepository ingredientRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final FcmService fcmService;

    /**
     * 매일 자정(새벽 12시), 소비기한이 3일 남은 식재료를 찾아 해당 냉장고 멤버들에게 알림을 보냅니다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void notifyExpiringIngredients() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        log.info("[Expiration Scheduler] ⏰ {}에 만료되는 식재료 스캔 시작...", targetDate);

        // 1. 3일 뒤 만료되는 식재료 전수 조사
        List<Ingredient> expiringIngredients = ingredientRepository.findByExpirationDate(targetDate);

        if (expiringIngredients.isEmpty()) {
            log.info("[Expiration Scheduler] ✅ 소비기한 임박 식재료가 없습니다.");
            return;
        }

        // 2. 냉장고별로 식재료 그룹화 (알림 최적화: 한 냉장고에 여러 개가 임박할 수 있음)
        Map<Fridge, List<Ingredient>> ingredientsByFridge = expiringIngredients.stream()
                .filter(i -> i.getFridge() != null)
                .collect(Collectors.groupingBy(Ingredient::getFridge));

        for (Map.Entry<Fridge, List<Ingredient>> entry : ingredientsByFridge.entrySet()) {
            Fridge fridge = entry.getKey();
            List<Ingredient> items = entry.getValue();

            // 3. 해당 냉장고를 사용하는 모든 가족(멤버) 조회
            List<User> members = fridgeMemberRepository.findByFridge(fridge).stream()
                    .map(FridgeMember::getUser)
                    .toList();

            if (members.isEmpty()) continue;

            // 4. 알림 메시지 생성 (예: "계란 외 2건의 소비기한이 3일 남았습니다.")
            String representativeItem = items.get(0).getName();
            String title = "⏰ [소비기한 임박] 냉장고를 확인하세요!";
            String body = items.size() > 1
                    ? String.format("'%s' 외 %d건의 소비기한이 3일 남았습니다. 빨리 드시는 게 좋겠어요!", representativeItem, items.size() - 1)
                    : String.format("'%s'의 소비기한이 3일 남았습니다. 신선할 때 요리해보세요!", representativeItem);

            // 5. 가족 전체에게 일괄 푸시 알림 전송
            try {
                fcmService.sendToUsers(members, title, body, "/fridge/" + fridge.getId());
                log.info("📱 냉장고 '{}' 멤버 {}명에게 소비기한 알림 전송 완료", fridge.getName(), members.size());
            } catch (Exception e) {
                log.error("📱 소비기한 알림 전송 중 오류 발생: {}", e.getMessage());
            }
        }
    }
}
