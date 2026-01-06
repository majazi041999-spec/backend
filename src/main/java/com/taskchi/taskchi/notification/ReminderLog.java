package com.taskchi.taskchi.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "reminder_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_minutes", columnNames = {"meeting_id", "minutes_before"})
)
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    @Getter
    @Setter
    private Long meetingId;

    @Column(name = "minutes_before", nullable = false)
    @Getter
    @Setter
    private Integer minutesBefore;

    @Column(nullable = false)
    @Getter
    @Setter
    private Instant firedAt;

    @PrePersist
    public void prePersist() {
        this.firedAt = Instant.now();
    }
}
