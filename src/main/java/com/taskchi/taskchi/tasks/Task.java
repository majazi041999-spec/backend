package com.taskchi.taskchi.tasks;

import com.taskchi.taskchi.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @Column(nullable = false, length = 300)
    @Getter @Setter
    private String title;

    @Column(columnDefinition = "text")
    @Getter @Setter
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Getter @Setter
    private TaskPriority priority = TaskPriority.P3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Getter @Setter
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_at")
    @Getter @Setter
    private OffsetDateTime dueAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    @Getter @Setter
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    @Getter @Setter
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Getter @Setter
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // getters/setters
}