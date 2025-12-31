package com.taskchi.taskchi.users;

import com.taskchi.taskchi.common.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public AdminUserController(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public record CreateUserRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotNull Role role
    ) {}

    public record UserDto(Long id, String fullName, String email, Role role, boolean active) {}

    public record CreateUserResponse(UserDto user, String initialPassword) {}

    @GetMapping
    public List<UserDto> list() {
        return repo.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive()))
                .toList();
    }

    @PostMapping
    public CreateUserResponse create(@RequestBody CreateUserRequest req) {
        if (repo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        String initialPassword = generatePassword(10);

        User u = new User();
        u.setFullName(req.fullName());
        u.setEmail(req.email());
        u.setRole(req.role());
        u.setActive(true);
        u.setPasswordHash(encoder.encode(initialPassword));

        User saved = repo.save(u);

        return new CreateUserResponse(
                new UserDto(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole(), saved.isActive()),
                initialPassword
        );
    }

    public record ResetPasswordResponse(String newPassword) {}

    @PatchMapping("/{id}/reset-password")
    public ResetPasswordResponse resetPassword(@PathVariable Long id) {
        User u = repo.findById(id).orElseThrow();
        String newPassword = generatePassword(10);
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
        return new ResetPasswordResponse(newPassword);
    }

    public record ActivateRequest(boolean active) {}

    @PatchMapping("/{id}/activate")
    public void activate(@PathVariable Long id, @RequestBody ActivateRequest req) {
        User u = repo.findById(id).orElseThrow();
        u.setActive(req.active());
        repo.save(u);
    }

    private String generatePassword(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
}
