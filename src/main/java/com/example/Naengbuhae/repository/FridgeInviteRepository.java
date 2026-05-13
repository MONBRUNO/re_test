package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FridgeInviteRepository extends JpaRepository<FridgeInvite, Long> {
    Optional<FridgeInvite> findByCode(String code);

    // 아직 만료되지 않은 활성 초대 (가장 늦게 만료되는 것 = 가장 최근 발급된 것).
    // "발급" 버튼을 다시 눌렀을 때 새 코드 만들지 않고 기존 활성 코드 재사용용.
    Optional<FridgeInvite> findFirstByFridgeAndExpiresAtAfterOrderByExpiresAtDesc(
            Fridge fridge, LocalDateTime now);

    void deleteByFridge(Fridge fridge);
}
