package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    // 특정 사용자가 등록한 레시피. user는 EntityGraph로 즉시 fetch(N+1 방지),
    // ingredients/steps는 application.properties의 batch_fetch_size로 묶어 로드.
    @EntityGraph(attributePaths = {"user"})
    List<Recipe> findByUser(User user);

    void deleteByUser(User user);

    // [최적화] 레시피와 유저를 한 방에 가져오는 조인 페치! (N+1 방지)
    @Query("SELECT r FROM Recipe r JOIN FETCH r.user")
    List<Recipe> findAllWithUser();

    // [추천용] 모든 레시피 + 작성자 + 재료까지 한 번에 가져오기
    // distinct로 컬렉션 join 시 중복 제거
    @Query("SELECT DISTINCT r FROM Recipe r JOIN FETCH r.user LEFT JOIN FETCH r.ingredients")
    List<Recipe> findAllWithUserAndIngredients();

    // ✨ N+1 방어막: 레시피를 가져올 때, '작성자(user)'와 '재료 목록(ingredients)'을 JOIN으로 한방에 끌어옵니다!
    // 이렇게 하면 레시피가 100개든 1000개든 쿼리가 딱 1번만 나갑니다.
    @EntityGraph(attributePaths = {"user", "ingredients"})
    @Query("SELECT r FROM Recipe r")
    List<Recipe> findAllOptimized();
}
