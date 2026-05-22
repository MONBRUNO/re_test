package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FridgeRepository extends JpaRepository<Fridge, UUID> {

    List<Fridge> findByOwner(User owner);

    // 탈퇴 시: 자식(멤버/식재료/활동로그/초대코드)을 모두 정리한 뒤 호출하는 즉시 벌크 DELETE.
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("delete from Fridge f where f.owner = :owner")
    void deleteAllByOwnerInBatch(@org.springframework.data.repository.query.Param("owner") User owner);

    // 특정 사용자가 멤버로 가입된 모든 냉장고 (소유 + 공유 받은 것 합쳐서).
    // ✨ [N+1 박멸] EntityGraph로 members + 그 안의 user + owner까지 한 방에 fetch join.
    //
    // ⚠️ 필터 조건(WHERE fm.user = :user)을 `JOIN f.members fm`으로 걸면 안 된다.
    //    @EntityGraph가 members를 fetch할 때 그 JOIN을 재사용해, fetch된 members 컬렉션이
    //    "호출자 본인 1건"으로만 채워진다 (각 멤버가 서로를 못 보는 버그).
    //    그래서 필터는 서브쿼리로 분리해 members fetch가 걸러지지 않게 한다.
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"members", "members.user", "owner"})
    @org.springframework.data.jpa.repository.Query(
            "SELECT DISTINCT f FROM Fridge f " +
            "WHERE f.id IN (SELECT fm.fridge.id FROM FridgeMember fm WHERE fm.user = :user) " +
            "ORDER BY f.id ASC"
    )
    List<Fridge> findAllForMember(@org.springframework.data.repository.query.Param("user") User user);
}
