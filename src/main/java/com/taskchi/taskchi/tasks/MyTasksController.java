package com.taskchi.taskchi.tasks;

import com.taskchi.taskchi.auth.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/tasks")
public class MyTasksController {
    private final TaskRepository repo;
    private final CurrentUser currentUser;
    private final TaskController taskController; // فقط برای toDto (یا یک Mapper جدا بسازیم)

    public MyTasksController(TaskRepository repo, CurrentUser currentUser, TaskController taskController) {
        this.repo = repo;
        this.currentUser = currentUser;
        this.taskController = taskController;
    }

    @GetMapping
    public List<TaskController.TaskDto> myTasks(@RequestParam String type, Authentication auth) {
        var me = currentUser.requireUser(auth);

        return switch (type) {
            case "assigned" -> repo.findAssignedTo(me.getId()).stream().map(taskController::toDto).toList();
            case "created" -> repo.findCreatedBy(me.getId()).stream().map(taskController::toDto).toList();
            case "done" -> repo.findDoneBy(me.getId()).stream().map(taskController::toDto).toList();
            default -> throw new IllegalArgumentException("type must be assigned|created|done");
        };
    }
}
