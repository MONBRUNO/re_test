package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    // 특정 사용자가 등록한 레시피만 가져오는 마법의 메서드!
    List<Recipe> findByUser(User user);
}
