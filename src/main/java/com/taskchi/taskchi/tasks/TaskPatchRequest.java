package com.taskchi.taskchi.tasks;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TaskPatchRequest {
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;

    // Follow-up reminder for the creator/assigner
    private Boolean followUpEnabled;
    private Instant followUpAt;
}
