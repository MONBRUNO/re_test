package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.domain.RecipeFavorite;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeFavoriteRepository extends JpaRepository<RecipeFavorite, Long> {

    boolean existsByUserAndRecipe(User user, Recipe recipe);

    @Modifying
    void deleteByUserAndRecipe(User user, Recipe recipe);

    // 즐겨찾기로 표시된 recipe id 집합 — RecipeService가 RecipeResponseDto.favorite 채울 때 사용.
    @Query("select rf.recipe.id from RecipeFavorite rf where rf.user = :user")
    List<Long> findRecipeIdsByUser(@Param("user") User user);

    @Modifying
    void deleteByUser(User user);

    @Modifying
    void deleteByRecipe(Recipe recipe);

    // 탈퇴 시: 탈퇴 사용자의 레시피를 즐겨찾기한 다른 사용자들의 row도 함께 정리해
    // recipe FK 위반을 미리 방어한다.
    @Modifying
    @Query("delete from RecipeFavorite rf where rf.recipe.user = :owner")
    void deleteByRecipeOwner(@Param("owner") User owner);
}
