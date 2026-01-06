package com.taskchi.taskchi.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    @Query("""
        select (count(r) > 0)
        from ReminderLog r
        where r.meetingId = :meetingId
          and r.minutesBefore = :minutesBefore
    """)
    boolean existsByMeetingIdAndMinutesBefore(
            @Param("meetingId") Long meetingId,
            @Param("minutesBefore") Integer minutesBefore
    );

    @Query("""
  select (count(r) > 0)
  from ReminderLog r
  where r.meetingId = :meetingId
    and r.minutesBefore = :minutesBefore
""")
    boolean existsOffset(@Param("meetingId") Long meetingId,
                         @Param("minutesBefore") Integer minutesBefore);
}