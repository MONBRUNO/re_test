package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.ActivityLog;
import com.example.Naengbuhae.domain.Fridge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // 멤버별 추가/삭제 카운트.
    // [actor_username, actor_name, action, count]
    @Query("""
            select a.actor.username, a.actor.name, a.action, count(a)
            from ActivityLog a
            where a.fridge = :fridge and a.createdAt >= :since
            group by a.actor.username, a.actor.name, a.action
            """)
    List<Object[]> countByActorAndAction(@Param("fridge") Fridge fridge,
                                         @Param("since") LocalDateTime since);

    // 식재료 이름별 카운트 (특정 action만).
    // [ingredient_name, count]
    @Query("""
            select a.ingredientName, count(a)
            from ActivityLog a
            where a.fridge = :fridge and a.action = :action and a.createdAt >= :since
            group by a.ingredientName
            order by count(a) desc
            """)
    List<Object[]> topIngredientsByAction(@Param("fridge") Fridge fridge,
                                          @Param("action") ActivityLog.Action action,
                                          @Param("since") LocalDateTime since);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM ActivityLog a WHERE a.fridge = :fridge")
    void deleteAllByFridgeInBatch(@Param("fridge") Fridge fridge);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM ActivityLog a WHERE a.actor = :actor")
    void deleteAllByActorInBatch(@Param("actor") com.example.Naengbuhae.user.User actor);
}
