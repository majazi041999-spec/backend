package com.taskchi.taskchi.tasks;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    @Query("select t from Task t where t.id = :id")
    Optional<Task> findWithUsersById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    @Query("select t from Task t")
    List<Task> findAllWithUsers();

    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    @Query("select t from Task t where t.assignee.id = :userId order by t.priority asc, t.createdAt desc")
    List<Task> findAssignedTo(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    @Query("select t from Task t where t.createdBy.id = :userId order by t.createdAt desc")
    List<Task> findCreatedBy(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"assignee", "createdBy"})
    @Query("select t from Task t where t.assignee.id = :userId and t.status = com.taskchi.taskchi.tasks.TaskStatus.DONE order by t.updatedAt desc")
    List<Task> findDoneBy(@Param("userId") Long userId);

}