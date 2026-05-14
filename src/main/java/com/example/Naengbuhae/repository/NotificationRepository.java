package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Notification;
import com.example.Naengbuhae.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 최신순. Pageable로 페이지 크기 제어.
    List<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndReadFalse(User user);

    @Modifying
    @Query("update Notification n set n.read = true where n.user = :user and n.read = false")
    int markAllAsRead(@Param("user") User user);

    void deleteByUser(User user);
}
