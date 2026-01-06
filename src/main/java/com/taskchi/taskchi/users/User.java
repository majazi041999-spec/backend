package com.taskchi.taskchi.users;

import com.taskchi.taskchi.common.Role;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role; // enum Role (ADMIN, STAFF, ...)

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager; // سلسله‌مراتب

    @OneToMany(mappedBy = "manager")
    private List<User> subordinates = new ArrayList<>();

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
