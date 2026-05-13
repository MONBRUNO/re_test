package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FridgeRepository extends JpaRepository<Fridge, Long> {

    List<Fridge> findByOwner(User owner);

    // 특정 사용자가 멤버로 가입된 모든 냉장고 (소유 + 공유 받은 것 합쳐서).
    // FridgeMember 테이블 join — owner도 자기 fridge의 멤버이므로 결과에 포함됨.
    @org.springframework.data.jpa.repository.Query(
            "SELECT f FROM Fridge f " +
            "JOIN FridgeMember fm ON fm.fridge = f " +
            "WHERE fm.user = :user " +
            "ORDER BY f.id ASC"
    )
    List<Fridge> findAllForMember(@org.springframework.data.repository.query.Param("user") User user);
}
