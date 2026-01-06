package com.taskchi.taskchi.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Meeting.reminderMinutesBefore is @ElementCollection (LAZY by default)
    // => must fetch explicitly to avoid LazyInitializationException in scheduler/controllers.

    @Query("""
            select distinct m
            from Meeting m
            left join fetch m.reminderMinutesBefore r
            left join fetch m.createdBy cb
            where m.date between :from and :to
            """)
    List<Meeting> findByDateBetweenFetchAll(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select distinct m
            from Meeting m
            left join fetch m.reminderMinutesBefore r
            left join fetch m.createdBy cb
            where cb.id = :createdById
              and m.date between :from and :to
            """)
    List<Meeting> findByCreatedByIdAndDateBetweenFetchAll(
            @Param("createdById") Long createdById,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
            select distinct m
            from Meeting m
            left join fetch m.reminderMinutesBefore r
            left join fetch m.createdBy cb
            where m.id = :id and cb.id = :createdById
            """)
    Optional<Meeting> findByIdAndCreatedByIdFetchAll(@Param("id") Long id, @Param("createdById") Long createdById);
}
