package com.taskchi.taskchi.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface TaskFollowUpLogRepository extends JpaRepository<TaskFollowUpLog, Long> {
    boolean existsByTaskIdAndFollowUpAt(Long taskId, Instant followUpAt);
}
