package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    // 특정 사용자의 식재료만 가져오는 마법의 메서드 추가!
    List<Ingredient> findByUser(User user);

    // 특정 냉장고에 속한 식재료. 가족 공유 시 같은 냉장고를 보는 모든 멤버에게 동일 결과.
    // ✨ [N+1 박멸] 등록자(user) 정보를 DTO에서 사용하므로 한 방에 fetch join!
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    List<Ingredient> findByFridge(Fridge fridge);

    // 마이그레이션용: 아직 냉장고가 지정되지 않은 사용자별 식재료.
    List<Ingredient> findByUserAndFridgeIsNull(User user);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM Ingredient i WHERE i.user = :user")
    void deleteAllByUserInBatch(@org.springframework.data.repository.query.Param("user") User user);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM Ingredient i WHERE i.fridge = :fridge")
    void deleteAllByFridgeInBatch(@org.springframework.data.repository.query.Param("fridge") Fridge fridge);

    // ✨ 4단계 핵심: 이름에 특정 키워드가 '포함된' 식재료를 전부 찾아오는 마법의 메서드!
    List<Ingredient> findByNameContaining(String keyword);

    // ✨ 소비기한 임박 알림용: 특정 날짜에 만료되는 식재료 조회
    List<Ingredient> findByExpirationDate(java.time.LocalDate date);
}
