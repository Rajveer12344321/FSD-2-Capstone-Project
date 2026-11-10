package com.bloodbridge.repo;

import com.bloodbridge.domain.Notification;
import com.bloodbridge.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndSeenFalse(User user);
    long countByUserAndSeenFalse(User user);
}
