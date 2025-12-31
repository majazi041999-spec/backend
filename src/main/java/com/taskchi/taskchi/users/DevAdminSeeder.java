package com.taskchi.taskchi.users;

import com.taskchi.taskchi.common.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

@Component
@Profile("dev")
public class DevAdminSeeder implements CommandLineRunner {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public DevAdminSeeder(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        User admin = new User();
        admin.setFullName("Admin");
        admin.setEmail("admin@taskchi.local");
        admin.setPasswordHash(encoder.encode("admin1234"));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        repo.save(admin);

        System.out.println("=== DEV ADMIN CREATED ===");
        System.out.println("email: admin@taskchi.local");
        System.out.println("pass : admin1234");
        System.out.println("=========================");
    }
}
