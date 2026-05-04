package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    // 특정 사용자가 등록한 레시피만 가져오는 마법의 메서드!
    List<Recipe> findByUser(User user);

    // [최적화] 레시피와 유저를 한 방에 가져오는 조인 페치! (N+1 방지)
    @Query("SELECT r FROM Recipe r JOIN FETCH r.user")
    List<Recipe> findAllWithUser();

    // [추천용] 모든 레시피 + 작성자 + 재료까지 한 번에 가져오기
    // distinct로 컬렉션 join 시 중복 제거
    @Query("SELECT DISTINCT r FROM Recipe r JOIN FETCH r.user LEFT JOIN FETCH r.ingredients")
    List<Recipe> findAllWithUserAndIngredients();
}
