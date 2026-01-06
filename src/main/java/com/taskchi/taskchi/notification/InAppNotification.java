package com.taskchi.taskchi.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taskchi.taskchi.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "in_app_notifications")
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String type; // e.g. MEETING_REMINDER

    @Column(nullable = false, length = 180)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Owner of the notification.
     * Nullable for backward compatibility if DB already has rows.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private Long meetingId;

    // Used for TASK_MESSAGE, TASK_FOLLOWUP, etc.
    private Long taskId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

}
