package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FridgeMemberRepository extends JpaRepository<FridgeMember, Long> {

    Optional<FridgeMember> findByFridgeAndUser(Fridge fridge, User user);

    List<FridgeMember> findByFridge(Fridge fridge);

    List<FridgeMember> findByUser(User user);

    boolean existsByFridgeAndUser(Fridge fridge, User user);

    void deleteByFridgeAndUser(Fridge fridge, User user);

    void deleteByFridge(Fridge fridge);
}
