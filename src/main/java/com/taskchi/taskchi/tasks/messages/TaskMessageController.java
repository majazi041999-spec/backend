package com.taskchi.taskchi.tasks.messages;

import com.taskchi.taskchi.auth.CurrentUser;
import com.taskchi.taskchi.notification.InAppNotification;
import com.taskchi.taskchi.notification.InAppNotificationRepository;
import com.taskchi.taskchi.tasks.Task;
import com.taskchi.taskchi.tasks.TaskRepository;
import com.taskchi.taskchi.users.User;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/**
 * In-task messaging between: assignee <-> creator (+ admin can monitor).
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin
public class TaskMessageController {

    private final TaskRepository taskRepo;
    private final TaskMessageRepository msgRepo;
    private final InAppNotificationRepository notifRepo;
    private final CurrentUser currentUser;
    private final EntityManager em;

    public TaskMessageController(
            TaskRepository taskRepo,
            TaskMessageRepository msgRepo,
            InAppNotificationRepository notifRepo,
            CurrentUser currentUser,
            EntityManager em
    ) {
        this.taskRepo = taskRepo;
        this.msgRepo = msgRepo;
        this.notifRepo = notifRepo;
        this.currentUser = currentUser;
        this.em = em;
    }

    public record TaskMessageDto(
            Long id,
            Long taskId,
            Long senderId,
            String senderName,
            String body,
            Instant createdAt
    ) { }

    public record CreateTaskMessageRequest(String body) { }

    @GetMapping("/{taskId}/messages")
    @Transactional(readOnly = true)
    public List<TaskMessageDto> list(@PathVariable Long taskId, Authentication auth) {
        User me = currentUser.requireUser(auth);

        Task task = taskRepo.findByIdWithPeople(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        if (!canAccessTask(me, task)) {
            throw new AccessDeniedException("Not allowed");
        }

        return msgRepo.findByTaskIdWithSender(taskId).stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/{taskId}/messages")
    @Transactional
    public TaskMessageDto create(@PathVariable Long taskId,
                                 @RequestBody CreateTaskMessageRequest req,
                                 Authentication auth) {
        User me = currentUser.requireUser(auth);

        Task task = taskRepo.findByIdWithPeople(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        if (!canAccessTask(me, task)) {
            throw new AccessDeniedException("Not allowed");
        }

        if (req == null || req.body() == null || req.body().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }

        String body = req.body().trim();
        if (body.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is too long");
        }

        TaskMessage m = TaskMessage.builder()
                .task(task)
                .sender(me)
                .body(body)
                .build();

        TaskMessage saved = msgRepo.save(m);

        // notify other participants
        createNotifications(task, me, body);

        return toDto(saved);
    }

    private TaskMessageDto toDto(TaskMessage m) {
        User s = m.getSender();
        return new TaskMessageDto(
                m.getId(),
                m.getTask() != null ? m.getTask().getId() : null,
                s != null ? s.getId() : null,
                s != null ? s.getFullName() : null,
                m.getBody(),
                m.getCreatedAt()
        );
    }

    private boolean canAccessTask(User me, Task task) {
        if (me.isAdmin()) return true;

        Long meId = me.getId();
        boolean isAssignee = task.getAssignedTo() != null && Objects.equals(task.getAssignedTo().getId(), meId);
        boolean isCreator = task.getCreatedBy() != null && Objects.equals(task.getCreatedBy().getId(), meId);

        return isAssignee || isCreator;
    }

    private void createNotifications(Task task, User sender, String body) {
        Set<Long> recipientIds = new LinkedHashSet<>();

        if (task.getCreatedBy() != null) recipientIds.add(task.getCreatedBy().getId());
        if (task.getAssignedTo() != null) recipientIds.add(task.getAssignedTo().getId());

        // remove sender
        if (sender != null && sender.getId() != null) recipientIds.remove(sender.getId());

        if (recipientIds.isEmpty()) return;

        String title = "پیام جدید درباره تسک: " + safe(task.getTitle());
        String preview = buildPreview(sender, body);

        for (Long rid : recipientIds) {
            User u = em.getReference(User.class, rid);

            InAppNotification n = new InAppNotification();
            n.setType("TASK_MESSAGE");
            n.setUser(u);
            n.setTaskId(task.getId());
            n.setTitle(title);
            n.setMessage(preview);

            notifRepo.save(n);
        }
    }

    private String buildPreview(User sender, String body) {
        String name = (sender == null || sender.getFullName() == null) ? "کاربر" : sender.getFullName();
        String text = body == null ? "" : body.replace("\n", " ").trim();
        if (text.length() > 220) text = text.substring(0, 220) + "…";
        return name + ": " + text;
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
