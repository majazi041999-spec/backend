package com.taskchi.taskchi.users;

import com.taskchi.taskchi.common.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    @Getter
    @Setter
    private String fullName;

    @Column(nullable = false, unique = true, length = 200)
    @Getter
    @Setter
    private String email;

    @Column(name = "password_hash", nullable = false, length = 200)
    @Getter
    @Setter
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Getter
    @Setter
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Getter
    @Setter
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Getter
    @Setter
    private OffsetDateTime createdAt = OffsetDateTime.now();

}
