package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.ShoppingItem;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
    // 내 장보기 목록 전체 가져오기 (수량/체크 변경 후에도 순서 유지 위해 ID ASC 강제)
    List<ShoppingItem> findByUserOrderByIdAsc(User user);

    // 내부 lookup용 — Set으로 변환해서 쓰는 곳(중복 검사/제안 필터)에선 순서 무관
    List<ShoppingItem> findByUser(User user);

    // ✨ 추가: 내 장보기 목록 중 '체크(구매 완료)'된 것만 싹 다 가져오기!
    List<ShoppingItem> findByUserAndCheckedTrue(User user);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM ShoppingItem s WHERE s.user = :user")
    void deleteAllByUserInBatch(@org.springframework.data.repository.query.Param("user") User user);
    }