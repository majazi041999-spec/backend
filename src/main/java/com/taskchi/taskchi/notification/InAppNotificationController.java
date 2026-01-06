package com.taskchi.taskchi.notification;

import com.taskchi.taskchi.auth.CurrentUser;
import com.taskchi.taskchi.users.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class InAppNotificationController {

    private final InAppNotificationRepository repo;
    private final CurrentUser currentUser;

    public InAppNotificationController(InAppNotificationRepository repo, CurrentUser currentUser) {
        this.repo = repo;
        this.currentUser = currentUser;
    }

    public record NotificationDto(
            Long id,
            String type,
            String title,
            String message,
            Long meetingId,
            Long taskId,
            Instant createdAt,
            Instant readAt
    ) {}

    @GetMapping
    public List<NotificationDto> listMine(Authentication auth) {
        User me = currentUser.requireUser(auth);
        return repo.findTop200ByUserIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(n -> new NotificationDto(
                        n.getId(),
                        n.getType(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getMeetingId(),
                        n.getTaskId(),
                        n.getCreatedAt(),
                        n.getReadAt()
                ))
                .toList();
    }

    @PostMapping("/{id}/read")
    @Transactional
    public void markRead(@PathVariable Long id, Authentication auth) {
        User me = currentUser.requireUser(auth);

        InAppNotification n = repo.findByIdWithUser(id).orElseThrow();

        // فقط صاحب اعلان یا ADMIN
        boolean allowed = me.isAdmin() || (n.getUser() != null && n.getUser().getId().equals(me.getId()));
        if (!allowed) throw new AccessDeniedException("Not allowed");

        n.setReadAt(Instant.now());
        repo.save(n);
    }
}
