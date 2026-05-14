package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.ActivityLog;
import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.repository.ActivityLogRepository;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 활동 로그 기록 + 통계 집계.
// FridgeService.ensureMember()와 중복되지 않도록 식재료 hook 쪽에서만 직접 호출.
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private static final int TOP_INGREDIENTS_LIMIT = 5;

    private final ActivityLogRepository activityLogRepository;
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recordIngredientAdded(Fridge fridge, User actor,
                                      String name, Double quantity, String unit) {
        activityLogRepository.save(new ActivityLog(
                fridge, actor, ActivityLog.Action.INGREDIENT_ADDED, name, quantity, unit));
    }

    @Transactional
    public void recordIngredientRemoved(Fridge fridge, User actor, String name) {
        activityLogRepository.save(new ActivityLog(
                fridge, actor, ActivityLog.Action.INGREDIENT_REMOVED, name, null, null));
    }

    // 통계 조회 — 기간(days) 내 멤버별 추가/삭제 카운트 + 자주 추가/삭제 TOP 5.
    @Transactional(readOnly = true)
    public Stats getStats(Long fridgeId, int days, String requesterUsername) {
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Fridge fridge = fridgeRepository.findById(fridgeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다."));
        if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, requester)) {
            throw new IllegalArgumentException("이 냉장고에 접근 권한이 없습니다.");
        }

        int safeDays = Math.max(1, Math.min(days, 365));
        LocalDateTime since = LocalDateTime.now().minusDays(safeDays);

        // 1) 멤버별 카운트 — 활동이 없는 멤버도 표시되어야 직관적이라 전체 멤버부터 0으로 초기화
        Map<String, MemberCount> byMember = new HashMap<>();
        for (FridgeMember fm : fridgeMemberRepository.findByFridge(fridge)) {
            User u = fm.getUser();
            byMember.put(u.getUsername(), new MemberCount(u.getUsername(), u.getName(), 0, 0));
        }
        for (Object[] row : activityLogRepository.countByActorAndAction(fridge, since)) {
            String username = (String) row[0];
            String name = (String) row[1];
            ActivityLog.Action action = (ActivityLog.Action) row[2];
            long count = ((Number) row[3]).longValue();
            MemberCount mc = byMember.computeIfAbsent(username,
                    k -> new MemberCount(username, name, 0, 0));
            if (action == ActivityLog.Action.INGREDIENT_ADDED) {
                mc.added = (int) count;
            } else {
                mc.removed = (int) count;
            }
        }

        // 2) TOP 5 식재료 (추가/삭제 각각)
        List<NameCount> topAdded = toTopList(
                activityLogRepository.topIngredientsByAction(
                        fridge, ActivityLog.Action.INGREDIENT_ADDED, since));
        List<NameCount> topRemoved = toTopList(
                activityLogRepository.topIngredientsByAction(
                        fridge, ActivityLog.Action.INGREDIENT_REMOVED, since));

        return new Stats(
                fridge.getId(),
                fridge.getName(),
                safeDays,
                new ArrayList<>(byMember.values()),
                topAdded,
                topRemoved
        );
    }

    private List<NameCount> toTopList(List<Object[]> rows) {
        List<NameCount> result = new ArrayList<>();
        int limit = Math.min(rows.size(), TOP_INGREDIENTS_LIMIT);
        for (int i = 0; i < limit; i++) {
            Object[] row = rows.get(i);
            result.add(new NameCount((String) row[0], ((Number) row[1]).longValue()));
        }
        return result;
    }

    // === DTO ===

    public static class Stats {
        public final Long fridgeId;
        public final String fridgeName;
        public final int periodDays;
        public final List<MemberCount> members;
        public final List<NameCount> topAdded;
        public final List<NameCount> topRemoved;

        public Stats(Long fridgeId, String fridgeName, int periodDays,
                     List<MemberCount> members,
                     List<NameCount> topAdded, List<NameCount> topRemoved) {
            this.fridgeId = fridgeId;
            this.fridgeName = fridgeName;
            this.periodDays = periodDays;
            this.members = members;
            this.topAdded = topAdded;
            this.topRemoved = topRemoved;
        }

        public Long getFridgeId() { return fridgeId; }
        public String getFridgeName() { return fridgeName; }
        public int getPeriodDays() { return periodDays; }
        public List<MemberCount> getMembers() { return members; }
        public List<NameCount> getTopAdded() { return topAdded; }
        public List<NameCount> getTopRemoved() { return topRemoved; }
    }

    public static class MemberCount {
        public final String username;
        public final String name;
        public int added;
        public int removed;

        public MemberCount(String username, String name, int added, int removed) {
            this.username = username;
            this.name = name;
            this.added = added;
            this.removed = removed;
        }

        public String getUsername() { return username; }
        public String getName() { return name; }
        public int getAdded() { return added; }
        public int getRemoved() { return removed; }
    }

    public static class NameCount {
        public final String name;
        public final long count;

        public NameCount(String name, long count) {
            this.name = name;
            this.count = count;
        }

        public String getName() { return name; }
        public long getCount() { return count; }
    }

}
