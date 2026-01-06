package com.taskchi.taskchi.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "task_followup_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_task_followup", columnNames = {"task_id", "follow_up_at"})
)
public class TaskFollowUpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;

    @Column(name = "task_id", nullable = false)
    @Getter
    @Setter
    private Long taskId;

    @Column(name = "follow_up_at", nullable = false)
    @Getter
    @Setter
    private Instant followUpAt;

    @Column(nullable = false)
    @Getter
    @Setter
    private Instant firedAt;

    @PrePersist
    public void prePersist() {
        this.firedAt = Instant.now();
    }
}
