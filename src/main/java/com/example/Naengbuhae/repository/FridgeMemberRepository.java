package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FridgeMemberRepository extends JpaRepository<FridgeMember, Long> {

    Optional<FridgeMember> findByFridgeAndUser(Fridge fridge, User user);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    List<FridgeMember> findByFridge(Fridge fridge);

    List<FridgeMember> findByUser(User user);

    boolean existsByFridgeAndUser(Fridge fridge, User user);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM FridgeMember fm WHERE fm.fridge = :fridge")
    void deleteAllByFridgeInBatch(@org.springframework.data.repository.query.Param("fridge") Fridge fridge);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM FridgeMember fm WHERE fm.fridge = :fridge AND fm.user = :user")
    void deleteByFridgeAndUserInBatch(@org.springframework.data.repository.query.Param("fridge") Fridge fridge, 
                                      @org.springframework.data.repository.query.Param("user") User user);
    
    // 호환성 유지용
    default void deleteByFridgeAndUser(Fridge fridge, User user) {
        deleteByFridgeAndUserInBatch(fridge, user);
    }
}
