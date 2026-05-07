package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.ShoppingItem;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
    // 내 장보기 목록 전체 가져오기
    List<ShoppingItem> findByUser(User user);

    // ✨ 추가: 내 장보기 목록 중 '체크(구매 완료)'된 것만 싹 다 가져오기!
    List<ShoppingItem> findByUserAndCheckedTrue(User user);

    void deleteByUser(User user);
}