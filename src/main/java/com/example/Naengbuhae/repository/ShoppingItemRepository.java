package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.ShoppingItem;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
    // 내 장보기 목록만 가져오기
    List<ShoppingItem> findByUser(User user);
}