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
    // 특정 사용자가 등록한 레시피. 단일 객체인 user만 한 방에 가져오고,
    // 컬렉션(ingredients, steps)은 batch_size(100) 설정으로 최적화 로드.
    @EntityGraph(attributePaths = {"user"})
    List<Recipe> findByUser(User user);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM Recipe r WHERE r.user = :user")
    void deleteAllByUserInBatch(@org.springframework.data.repository.query.Param("user") User user);

    // [최적화] 레시피와 작성자(User)를 한 방에 가져오기 (N+1 방지)

    // 컬렉션은 batch_size 설정에 따라 필요할 때 IN 쿼리로 묶어서 가져옴.
    @Query("SELECT r FROM Recipe r JOIN FETCH r.user")
    List<Recipe> findAllWithUser();

    // [추천용] 작성자(User)만 한 방에 가져오고, 재료는 배치 페치 활용
    @Query("SELECT r FROM Recipe r JOIN FETCH r.user")
    List<Recipe> findAllWithUserAndIngredients();

    // ✨ [N+1 & MultipleBagFetch 방어]
    // 두 개 이상의 컬렉션을 Fetch Join하면 Cartesian Product가 발생하여 메모리 폭발 위험이 있음.
    // ManyToOne인 user만 Fetch Join하고, 컬렉션은 default_batch_fetch_size=100 설정으로 IN 쿼리 처리함.
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT r FROM Recipe r")
    List<Recipe> findAllOptimized();
}
