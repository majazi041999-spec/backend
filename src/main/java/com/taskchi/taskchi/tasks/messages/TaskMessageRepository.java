package com.taskchi.taskchi.tasks.messages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskMessageRepository extends JpaRepository<TaskMessage, Long> {

    @Query("""
        select m from TaskMessage m
        join fetch m.sender s
        where m.task.id = :taskId
        order by m.createdAt asc
    """)
    List<TaskMessage> findByTaskIdWithSender(@Param("taskId") Long taskId);
}
