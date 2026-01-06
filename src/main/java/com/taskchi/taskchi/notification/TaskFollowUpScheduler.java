package com.taskchi.taskchi.notification;

import com.taskchi.taskchi.tasks.Task;
import com.taskchi.taskchi.tasks.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class TaskFollowUpScheduler {

    private final TaskRepository taskRepo;
    private final InAppNotificationRepository notifRepo;
    private final TaskFollowUpLogRepository logRepo;
    private final ZoneId zoneId;

    public TaskFollowUpScheduler(
            TaskRepository taskRepo,
            InAppNotificationRepository notifRepo,
            TaskFollowUpLogRepository logRepo,
            @Value("${taskchi.default-zone:Asia/Tehran}") String zone
    ) {
        this.taskRepo = taskRepo;
        this.notifRepo = notifRepo;
        this.logRepo = logRepo;
        this.zoneId = ZoneId.of(zone);
    }

    // every 60 seconds
    @Scheduled(fixedDelay = 60_000)
    public void run() {
        Instant now = Instant.now();
        Instant min = now.minus(Duration.ofHours(48));

        List<Task> due = taskRepo.findDueFollowUps(now, min);

        for (Task t : due) {
            if (t.getCreatedBy() == null) continue;
            if (t.getFollowUpAt() == null) continue;
            if (!t.isFollowUpEnabled()) continue;

            Instant followUpAt = t.getFollowUpAt();
            if (logRepo.existsByTaskIdAndFollowUpAt(t.getId(), followUpAt)) continue;

            // Build notification
            InAppNotification n = new InAppNotification();
            n.setType("TASK_FOLLOWUP");
            n.setUser(t.getCreatedBy());
            n.setTaskId(t.getId());

            String title = "یادآوری پیگیری تسک: " + safe(t.getTitle());
            n.setTitle(title);
            n.setMessage(buildMessage(t, followUpAt));

            notifRepo.save(n);

            // Log to avoid duplicates
            TaskFollowUpLog log = new TaskFollowUpLog();
            log.setTaskId(t.getId());
            log.setFollowUpAt(followUpAt);
            logRepo.save(log);
        }
    }

    private String buildMessage(Task t, Instant followUpAt) {
        ZonedDateTime z = followUpAt.atZone(zoneId);
        String when = z.toLocalDate().toString() + " " + z.toLocalTime().withSecond(0).withNano(0);

        String assignee = (t.getAssignedTo() == null || t.getAssignedTo().getFullName() == null)
                ? "(نامشخص)"
                : t.getAssignedTo().getFullName();

        return "این تسک به \"" + assignee + "\" ارجاع داده شده است.\n"
                + "زمان پیگیری: " + when + "\n"
                + "برای مشاهده/پیگیری، وارد همان تسک شوید.";
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
