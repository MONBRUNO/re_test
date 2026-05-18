package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FridgeRepository extends JpaRepository<Fridge, UUID> {

    List<Fridge> findByOwner(User owner);

    // 특정 사용자가 멤버로 가입된 모든 냉장고 (소유 + 공유 받은 것 합쳐서).
    // FridgeMember 테이블 join — owner도 자기 fridge의 멤버이므로 결과에 포함됨.
    // ✨ [N+1 박멸] EntityGraph를 사용하여 members와 그 안의 user까지 한 방에 fetch join!
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"members", "members.user", "owner"})
    @org.springframework.data.jpa.repository.Query(
            "SELECT DISTINCT f FROM Fridge f " +
            "JOIN f.members fm " +
            "WHERE fm.user = :user " +
            "ORDER BY f.id ASC"
    )
    List<Fridge> findAllForMember(@org.springframework.data.repository.query.Param("user") User user);
}
