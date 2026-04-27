package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    // 특정 사용자의 식재료만 가져오는 마법의 메서드 추가!
    List<Ingredient> findByUser(User user);
}
