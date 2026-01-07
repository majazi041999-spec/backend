// backend/src/main/java/com/taskchi/taskchi/meeting/MeetingRepository.java
package com.taskchi.taskchi.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // NOTE: ElementCollection (reminderMinutesBefore) is LAZY by default.
    // For any flows that need reminders (API DTO or scheduler), use the fetch-join queries below.

    List<Meeting> findByDateBetween(LocalDate from, LocalDate to);

    List<Meeting> findByCreatedByIdAndDateBetween(Long createdById, LocalDate from, LocalDate to);

    Optional<Meeting> findByIdAndCreatedById(Long id, Long createdById);

    @Query("""
        select distinct m from Meeting m
        left join fetch m.createdBy cb
        left join fetch m.reminderMinutesBefore r
        where m.date between :from and :to
        order by m.date asc
    """)
    List<Meeting> findInRangeWithReminders(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        select distinct m from Meeting m
        left join fetch m.createdBy cb
        left join fetch m.reminderMinutesBefore r
        where cb.id = :createdById
          and m.date between :from and :to
        order by m.date asc
    """)
    List<Meeting> findByCreatedByIdInRangeWithReminders(
            @Param("createdById") Long createdById,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
        select distinct m from Meeting m
        left join fetch m.createdBy cb
        left join fetch m.reminderMinutesBefore r
        where m.id = :id
          and cb.id = :createdById
    """)
    Optional<Meeting> findByIdAndCreatedByIdWithReminders(
            @Param("id") Long id,
            @Param("createdById") Long createdById
    );
}
