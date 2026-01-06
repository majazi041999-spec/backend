package com.taskchi.taskchi.tasks;

import com.taskchi.taskchi.auth.CurrentUser;
import com.taskchi.taskchi.users.User;
import com.taskchi.taskchi.users.UserHierarchyService;
import com.taskchi.taskchi.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin
public class TaskController {

    private final TaskRepository repo;
    private final UserRepository userRepo;
    private final UserHierarchyService hierarchy;
    private final CurrentUser currentUser;

    public TaskController(TaskRepository repo,
                          UserRepository userRepo,
                          UserHierarchyService hierarchy,
                          CurrentUser currentUser) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.hierarchy = hierarchy;
        this.currentUser = currentUser;
    }

    public record TaskDto(
            Long id,
            String title,
            String status,
            String priority,
            LocalDate date,
            Long assignedToId,
            String assignedToName,
            Long createdById,
            String createdByName,
            Boolean followUpEnabled,
            Instant followUpAt
    ) {}

    public TaskDto toDto(Task t) {
        Long assignedToId = t.getAssignedTo() != null ? t.getAssignedTo().getId() : null;
        String assignedToName = t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null;

        Long createdById = t.getCreatedBy() != null ? t.getCreatedBy().getId() : null;
        String createdByName = t.getCreatedBy() != null ? t.getCreatedBy().getFullName() : null;

        return new TaskDto(
                t.getId(),
                t.getTitle(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getPriority() != null ? t.getPriority().name() : null,
                t.getDate(),
                assignedToId,
                assignedToName,
                createdById,
                createdByName,
                t.isFollowUpEnabled(),
                t.getFollowUpAt()
        );
    }

    // 👁️ لیست تسک‌های قابل مشاهده (برای خود کاربر)
    @GetMapping
    @Transactional(readOnly = true)
    public List<TaskDto> list(Authentication auth) {
        User me = currentUser.requireUser(auth);
        return repo.findVisible(me.getId()).stream().map(this::toDto).toList();
    }

    // ➕ ساخت تسک
    @PostMapping
    @Transactional
    public TaskDto create(@RequestBody Task body, Authentication auth) {
        User me = currentUser.requireUser(auth);

        if (body.getAssignedTo() == null || body.getAssignedTo().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignedTo.id is required");
        }

        User assignee = userRepo.findById(body.getAssignedTo().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignee not found"));

        // ❌ کسی نمی‌تواند به ادمین تسک بده (ادمین فقط می‌تواند برای خودش تسک بسازد)
        if (assignee.isAdmin() && !assignee.getId().equals(me.getId())) {
            throw new AccessDeniedException("Cannot assign tasks to admin");
        }

        // اجازه‌ی ارجاع: ADMIN یا مدیر مستقیم/بالاسری (اگر hierarchy داری)
        if (!me.isAdmin()) {
            boolean canAssign = hierarchy.isManagerOf(me.getId(), assignee.getId()) || me.getId().equals(assignee.getId());
            if (!canAssign) throw new AccessDeniedException("Not allowed to assign to this user");
        }

        Task t = new Task();
        t.setTitle(body.getTitle());
        t.setDate(body.getDate());
        t.setAssignedTo(assignee);
        t.setCreatedBy(me);
        t.setStatus(body.getStatus() != null ? body.getStatus() : TaskStatus.TODO);
        t.setPriority(body.getPriority() != null ? body.getPriority() : TaskPriority.MEDIUM);

        // Follow-up reminder (for the creator/assigner)
        if (body.getFollowUpAt() != null) {
            t.setFollowUpAt(body.getFollowUpAt());
            t.setFollowUpEnabled(true);
        } else if (body.isFollowUpEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "followUpAt is required when followUpEnabled=true");
        }

        Task saved = repo.save(t);
        return toDto(saved);
    }

    @PatchMapping("/{id}")
    @Transactional
    public TaskDto patch(@PathVariable Long id, @RequestBody TaskPatchRequest req, Authentication auth) {
        User me = currentUser.requireUser(auth);

        Task t = repo.findByIdWithPeople(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        boolean isAssignee = t.getAssignedTo() != null && t.getAssignedTo().getId().equals(me.getId());
        boolean isCreator = t.getCreatedBy() != null && t.getCreatedBy().getId().equals(me.getId());

        // ✅ فقط: assignee یا creator یا admin
        if (!(me.isAdmin() || isAssignee || isCreator)) {
            throw new AccessDeniedException("Not allowed to update this task");
        }

        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }

        // ✅ assignee فقط می‌تواند status را تغییر دهد
        if (isAssignee && !me.isAdmin() && !isCreator) {
            if (req.getStatus() != null) {
                t.setStatus(req.getStatus());
            }
            // هر فیلد دیگری ممنوع
            if (req.getPriority() != null || req.getAssigneeId() != null
                    || req.getFollowUpEnabled() != null || req.getFollowUpAt() != null) {
                throw new AccessDeniedException("Assignee can only update status");
            }
        } else {
            // ✅ creator/admin می‌تواند status/priority را تغییر دهد
            if (req.getStatus() != null) t.setStatus(req.getStatus());
            if (req.getPriority() != null) t.setPriority(req.getPriority());

            // ✅ creator/admin می‌تواند assignee را تغییر دهد (با رعایت سلسله‌مراتب)
            if (req.getAssigneeId() != null) {
                User assignee = userRepo.findById(req.getAssigneeId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignee not found"));

                // ❌ کسی نمی‌تواند به ادمین تسک بده (ادمین فقط برای خودش)
                if (assignee.isAdmin() && !assignee.getId().equals(me.getId())) {
                    throw new AccessDeniedException("Cannot assign tasks to admin");
                }

                if (!me.isAdmin()) {
                    boolean canAssign = hierarchy.isManagerOf(me.getId(), assignee.getId()) || me.getId().equals(assignee.getId());
                    if (!canAssign) throw new AccessDeniedException("Not allowed to assign to this user");
                }

                t.setAssignedTo(assignee);
            }

            // Follow-up reminder (creator/admin only)
            if (req.getFollowUpEnabled() != null) {
                boolean enabled = Boolean.TRUE.equals(req.getFollowUpEnabled());
                t.setFollowUpEnabled(enabled);
                if (!enabled) {
                    t.setFollowUpAt(null);
                }
            }
            if (req.getFollowUpAt() != null) {
                t.setFollowUpAt(req.getFollowUpAt());
                t.setFollowUpEnabled(true); // setting time implies enabling
            }

            if (t.isFollowUpEnabled() && t.getFollowUpAt() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "followUpAt is required when followUpEnabled=true");
            }
        }

        Task saved = repo.save(t);
        return toDto(saved);
    }

    // 🗑️ حذف تسک: فقط creator یا admin
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        User me = currentUser.requireUser(auth);

        Task t = repo.findByIdWithPeople(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        if (!me.isAdmin() && (t.getCreatedBy() == null || !t.getCreatedBy().getId().equals(me.getId()))) {
            throw new AccessDeniedException("Only creator or admin can delete task");
        }

        repo.delete(t);
        return ResponseEntity.noContent().build();
    }
}
