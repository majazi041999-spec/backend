package com.taskchi.taskchi.meeting;

import com.taskchi.taskchi.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime startTime;
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean allDay = true;


    /**
     * Whether meeting reminders/alarms are enabled for this meeting.
     * If false, no reminders will be sent (including the mandatory same-day alert).
     *
     * Default: true
     */
    @Column(nullable = false)
    private boolean alarmEnabled = true;




    @Column(length = 180)
    private String location;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content; // full notes

    @Lob
    @Column(columnDefinition = "TEXT")
    private String outcome; // conclusion / action items

    /**
     * Owner of the meeting (the user who created it).
     * Nullable for backward compatibility if DB already has rows.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    // Store reminder offsets in minutes before meeting start (e.g., 2 days = 2880)
    @ElementCollection
    @CollectionTable(name = "meeting_reminders", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "minutes_before", nullable = false)
    @OrderColumn(name = "sort_index")
    private List<Integer> reminderMinutesBefore = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

}
