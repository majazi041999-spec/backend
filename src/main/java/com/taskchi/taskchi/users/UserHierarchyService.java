package com.taskchi.taskchi.users;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserHierarchyService {

    private final UserRepository userRepository;

    public UserHierarchyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSubordinate(Long managerId, Long targetId) {
        if (Objects.equals(managerId, targetId)) return true;
        Set<Long> visited = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(managerId);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            if (visited.contains(cur)) continue;
            visited.add(cur);
            List<User> subs = userRepository.findByManagerId(cur);
            for (User s : subs) {
                if (Objects.equals(s.getId(), targetId)) return true;
                queue.add(s.getId());
            }
        }
        return false;
    }

    public boolean isManagerOf(Long managerId, Long employeeId) {
        if (managerId == null || employeeId == null) return false;
        if (Objects.equals(managerId, employeeId)) return false; // خودِ شخص مدیرِ خودش حساب نمی‌شود
        return isSubordinate(managerId, employeeId);
    }

    /**
     * اگر employee را زیرمجموعه‌ی newManager کنیم، آیا حلقه (cycle) ایجاد می‌شود؟
     * مثال: employee مدیرِ newManager باشد، در این صورت newManager نمی‌تواند مدیر employee شود.
     */
    public boolean wouldCreateCycle(Long employeeId, Long newManagerId) {
        if (employeeId == null || newManagerId == null) return false;
        if (Objects.equals(employeeId, newManagerId)) return true;
        // اگر newManager در زیرمجموعه‌ی employee باشد => حلقه
        return isSubordinate(employeeId, newManagerId);
    }

}
