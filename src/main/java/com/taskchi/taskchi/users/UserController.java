package com.taskchi.taskchi.users;

import com.taskchi.taskchi.auth.CurrentUser;
import com.taskchi.taskchi.common.Role;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public UserController(UserRepository userRepository, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    public record UserMiniDto(Long id, String fullName, String email, Role role, boolean active) {}

    /**
     * لیست کاربرانی که کاربر لاگین‌شده می‌تواند به آن‌ها تسک ارجاع دهد.
     * - STAFF/Manager: خودش + زیرمجموعه‌ها
     * - ADMIN: همه‌ی کاربران فعال غیرادمین + خودش (ادمین‌های دیگر نمایش داده نمی‌شوند)
     */
    @GetMapping("/assignable")
    @Transactional(readOnly = true)
    public List<UserMiniDto> assignable(Authentication auth) {
        User me = currentUser.requireUser(auth);

        List<User> out = new ArrayList<>();

        if (me.isAdmin()) {
            for (User u : userRepository.findAll()) {
                if (!u.isActive()) continue;
                if (u.isAdmin() && !Objects.equals(u.getId(), me.getId())) continue;
                out.add(u);
            }
        } else {
            // self
            if (me.isActive()) out.add(me);

            // BFS on hierarchy by manager_id
            Set<Long> visited = new HashSet<>();
            Deque<Long> queue = new ArrayDeque<>();
            visited.add(me.getId());
            queue.add(me.getId());

            while (!queue.isEmpty()) {
                Long curId = queue.poll();
                List<User> subs = userRepository.findByManagerId(curId);
                for (User s : subs) {
                    if (s == null || s.getId() == null) continue;
                    if (!visited.add(s.getId())) continue;
                    queue.add(s.getId());

                    if (!s.isActive()) continue;
                    if (s.isAdmin()) continue; // هیچ‌کس به ادمین تسک نمی‌دهد
                    out.add(s);
                }
            }
        }

        out.sort(Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)));

        return out.stream()
                .map(u -> new UserMiniDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive()))
                .toList();
    }
}
