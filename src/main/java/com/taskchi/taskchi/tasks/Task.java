// backend/src/main/java/com/taskchi/taskchi/tasks/Task.java
package com.taskchi.taskchi.tasks;

import com.taskchi.taskchi.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    // Close workflow
    @Column(name = "close_requested", nullable = false)
    private boolean closeRequested = false;

    @Column(name = "close_requested_at")
    private Instant closeRequestedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_id")
    private User closedBy;

    // Reminder for the task creator/assigner to follow-up.
    @Column(name = "follow_up_enabled", nullable = false)
    private boolean followUpEnabled = false;

    @Column(name = "follow_up_at")
    private Instant followUpAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
