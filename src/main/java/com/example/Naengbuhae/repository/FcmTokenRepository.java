package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.FcmToken;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUser(User user);

    List<FcmToken> findByUserIn(List<User> users);

    Optional<FcmToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUser(User user);
}
