// backend/src/main/java/com/taskchi/taskchi/tasks/TaskRepository.java
package com.taskchi.taskchi.tasks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
        select t from Task t
        left join fetch t.assignedTo a
        left join fetch t.createdBy c
        left join fetch t.closedBy cb
        where t.id = :id
    """)
    Optional<Task> findByIdWithPeople(@Param("id") Long id);

    @Query("""
        select t from Task t
        left join fetch t.assignedTo a
        left join fetch t.createdBy c
        left join fetch t.closedBy cb
        where a.id = :userId
        order by t.createdAt desc
    """)
    List<Task> findAssignedTo(@Param("userId") Long userId);

    @Query("""
        select t from Task t
        left join fetch t.assignedTo a
        left join fetch t.createdBy c
        left join fetch t.closedBy cb
        where c.id = :userId
        order by t.createdAt desc
    """)
    List<Task> findCreatedBy(@Param("userId") Long userId);

    @Query("""
        select distinct t from Task t
        left join fetch t.assignedTo a
        left join fetch t.createdBy c
        left join fetch t.closedBy cb
        where a.id = :userId or c.id = :userId
        order by t.createdAt desc
    """)
    List<Task> findVisible(@Param("userId") Long userId);

    @Query("""
        select t from Task t
        left join fetch t.assignedTo a
        left join fetch t.createdBy c
        left join fetch t.closedBy cb
        where a.id = :userId
          and t.status = 'DONE'
        order by t.createdAt desc
    """)
    List<Task> findDoneBy(@Param("userId") Long userId);

    @Query("""
        select t from Task t
        left join fetch t.assignedTo a
        left join fetch t.createdBy c
        left join fetch t.closedBy cb
        where t.followUpEnabled = true
          and t.followUpAt is not null
          and t.followUpAt <= :now
          and t.followUpAt >= :min
          and t.status <> 'DONE'
        order by t.followUpAt asc
    """)
    List<Task> findDueFollowUps(@Param("now") Instant now, @Param("min") Instant min);
}
