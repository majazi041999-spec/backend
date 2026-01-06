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
    private final UserHierarchyService hierarchy;
    private final SecureRandom random = new SecureRandom();

    public AdminUserController(UserRepository repo, PasswordEncoder encoder, UserHierarchyService hierarchy) {
        this.repo = repo;
        this.encoder = encoder;
        this.hierarchy = hierarchy;
    }

    public record CreateUserRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotNull Role role
    ) {
    }

    public record UserDto(Long id, String fullName, String email, Role role, boolean active, Long managerId) {
    }

    public record CreateUserResponse(UserDto user, String initialPassword) {
    }

    @GetMapping
    public List<UserDto> list() {
        // با fetch join مدیر را هم می‌گیریم تا در صورت نیاز managerId بدون Lazy مشکل خوانده شود
        return repo.findAllWithManager().stream()
                .map(u -> new UserDto(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole(),
                        u.isActive(),
                        u.getManager() == null ? null : u.getManager().getId()
                ))
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
                new UserDto(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole(), saved.isActive(),
                        saved.getManager() == null ? null : saved.getManager().getId()),
                initialPassword
        );
    }

    public record SetManagerRequest(Long managerId) {
    }

    /**
     * تعیین مدیرِ یک کاربر (برای ساختن ساختار سلسله‌مراتبی).
     * managerId می‌تواند null باشد (یعنی کاربر مدیر ندارد).
     */
    @PatchMapping("/{id}/manager")
    public void setManager(@PathVariable Long id, @RequestBody SetManagerRequest req) {
        User u = repo.findById(id).orElseThrow();

        Long managerId = req.managerId();
        if (managerId == null) {
            u.setManager(null);
            repo.save(u);
            return;
        }

        if (id.equals(managerId)) {
            throw new IllegalArgumentException("User cannot be their own manager");
        }

        User manager = repo.findById(managerId).orElseThrow();

        // جلوگیری از حلقه: مدیر جدید نباید داخل زیرمجموعه‌ی همین کاربر باشد
        if (hierarchy.wouldCreateCycle(id, managerId)) {
            throw new IllegalArgumentException("Invalid manager assignment (cycle detected)");
        }

        u.setManager(manager);
        repo.save(u);
    }

    public record ResetPasswordResponse(String newPassword) {
    }

    @PatchMapping("/{id}/reset-password")
    public ResetPasswordResponse resetPassword(@PathVariable Long id) {
        User u = repo.findById(id).orElseThrow();
        String newPassword = generatePassword(10);
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
        return new ResetPasswordResponse(newPassword);
    }

    public record ActivateRequest(boolean active) {
    }

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

    public record UpdateUserRequest(
            String fullName,
            String email,
            Role role,
            Boolean active
    ) {
    }

    @PatchMapping("/{id}")
    public UserDto update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        User u = repo.findById(id).orElseThrow();

        // محافظت از ادمین اصلی (اختیاری ولی توصیه‌شده)
        if (u.getId().equals(1L) && req.role() != null && req.role() != Role.ADMIN) {
            throw new IllegalArgumentException("Main admin role cannot be changed");
        }

        if (req.fullName() != null && !req.fullName().trim().isEmpty()) {
            u.setFullName(req.fullName().trim());
        }

        if (req.email() != null && !req.email().trim().isEmpty()) {
            String newEmail = req.email().trim().toLowerCase();
            repo.findByEmail(newEmail).ifPresent(exist -> {
                if (!exist.getId().equals(u.getId())) {
                    throw new IllegalArgumentException("Email already exists");
                }
            });
            u.setEmail(newEmail);
        }

        if (req.role() != null) {
            u.setRole(req.role());
        }

        if (req.active() != null) {
            u.setActive(req.active());
        }

        User saved = repo.save(u);
        return new UserDto(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getRole(),
                saved.isActive(),
                saved.getManager() == null ? null : saved.getManager().getId()
        );
    }


    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        User u = repo.findById(id).orElseThrow();

        // محافظت از ادمین اصلی
        if (u.getId().equals(1L)) {
            throw new IllegalArgumentException("Main admin cannot be deleted");
        }

        // Soft delete
        u.setActive(false);
        // ایمیل را تغییر بده تا دوباره قابل استفاده باشد و کاربر دیگر لاگین نشود
        u.setEmail("deleted+" + u.getId() + "@taskchi.local");
        u.setFullName(u.getFullName() + " (deleted)");

        repo.save(u);
    }
}
