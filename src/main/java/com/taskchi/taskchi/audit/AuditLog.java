package com.taskchi.taskchi.audit;

import com.taskchi.taskchi.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    @Getter @Setter
    private User actorUser;

    @Column(name = "entity_type", nullable = false, length = 50)
    @Getter @Setter
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 50)
    @Getter @Setter
    private String entityId;

    @Column(nullable = false, length = 50)
    @Getter @Setter
    private String action;

    @Column(name = "before_json", columnDefinition = "jsonb")
    @Getter @Setter
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "jsonb")
    @Getter @Setter
    private String afterJson;

    @Column(length = 100)
    @Getter @Setter
    private String ip;

    @Column(name = "user_agent", length = 300)
    @Getter @Setter
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    @Getter @Setter
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // getters/setters
}
