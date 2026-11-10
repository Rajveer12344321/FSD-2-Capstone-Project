package com.bloodbridge.web;

import com.bloodbridge.domain.Notification;
import com.bloodbridge.domain.User;
import com.bloodbridge.repo.NotificationRepository;
import com.bloodbridge.web.Dtos.NotificationDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<NotificationDto> list(@AuthenticationPrincipal User user) {
        return notifications.findTop50ByUserOrderByCreatedAtDesc(user).stream()
                .map(n -> new NotificationDto(n.getId(), n.getMessage(), n.isSeen(), n.getCreatedAt())).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal User user) {
        return Map.of("count", notifications.countByUserAndSeenFalse(user));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public void read(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Notification n = notifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        n.setSeen(true);
        notifications.save(n);
    }

    @PostMapping("/read-all")
    @Transactional
    public void readAll(@AuthenticationPrincipal User user) {
        List<Notification> unread = notifications.findByUserAndSeenFalse(user);
        unread.forEach(n -> n.setSeen(true));
        notifications.saveAll(unread);
    }
}
