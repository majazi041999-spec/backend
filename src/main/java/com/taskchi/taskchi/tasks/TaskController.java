package com.taskchi.taskchi.tasks;

import com.taskchi.taskchi.auth.CurrentUser;
import com.taskchi.taskchi.users.User;
import com.taskchi.taskchi.users.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

import com.taskchi.taskchi.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public TaskController(TaskRepository taskRepo, UserRepository userRepo, CurrentUser currentUser, AuditService audit) {
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    public record CreateTaskRequest(
            @NotBlank String title,
            String description,
            TaskPriority priority,
            OffsetDateTime dueAt,
            Long assigneeId
    ) {
    }

    public record TaskDto(
            Long id,
            String title,
            String description,
            TaskPriority priority,
            TaskStatus status,
            OffsetDateTime dueAt,
            Long assigneeId,
            String assigneeName,
            Long createdById,
            String createdByName,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record UpdateTaskRequest(
            String title,
            String description,
            TaskPriority priority,
            TaskStatus status,
            OffsetDateTime dueAt,
            Long assigneeId
    ) {
    }

    @PatchMapping("/{id}")
    public TaskDto update(@PathVariable Long id,
                          @RequestBody UpdateTaskRequest req,
                          Authentication auth,
                          jakarta.servlet.http.HttpServletRequest httpReq) {

        User actor = currentUser.requireUser(auth);

        Task t = taskRepo.findWithUsersById(id).orElseThrow();
        TaskDto before = toDto(t);

        if (req.title() != null) t.setTitle(req.title());
        if (req.description() != null) t.setDescription(req.description());
        if (req.priority() != null) t.setPriority(req.priority());
        if (req.status() != null) t.setStatus(req.status());
        if (req.dueAt() != null) t.setDueAt(req.dueAt());

        if (req.assigneeId() != null) {
            User assignee = userRepo.findById(req.assigneeId()).orElseThrow();
            t.setAssignee(assignee);
        }

        taskRepo.save(t);

        Task reloaded = taskRepo.findWithUsersById(id).orElseThrow();
        TaskDto after = toDto(reloaded);

        audit.log(actor, "TASK", String.valueOf(id), "UPDATE", before, after, httpReq);
        return after;
    }

    @PostMapping
    public TaskDto create(@RequestBody CreateTaskRequest req,
                          Authentication auth,
                          jakarta.servlet.http.HttpServletRequest httpReq) {

        User creator = currentUser.requireUser(auth);

        Task t = new Task();
        t.setTitle(req.title());
        t.setDescription(req.description());
        if (req.priority() != null) t.setPriority(req.priority());
        t.setDueAt(req.dueAt());
        t.setCreatedBy(creator);

        if (req.assigneeId() != null) {
            User assignee = userRepo.findById(req.assigneeId()).orElseThrow();
            t.setAssignee(assignee);
        }

        Task saved = taskRepo.save(t);

        Task reloaded = taskRepo.findWithUsersById(saved.getId()).orElseThrow();
        TaskDto dto = toDto(reloaded);

        audit.log(creator, "TASK", String.valueOf(saved.getId()), "CREATE", null, dto, httpReq);
        return dto;
    }

    @GetMapping
    public List<TaskDto> list() {
        return taskRepo.findAllWithUsers().stream().map(this::toDto).toList();
    }

    TaskDto toDto(Task t) {
        return new TaskDto(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus(),
                t.getDueAt(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getAssignee() != null ? t.getAssignee().getFullName() : null,
                t.getCreatedBy().getId(),
                t.getCreatedBy().getFullName(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
