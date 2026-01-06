package com.taskchi.taskchi.notification;

import com.taskchi.taskchi.meeting.Meeting;
import com.taskchi.taskchi.meeting.MeetingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Component
public class ReminderScheduler {

    private final MeetingRepository meetingRepo;
    private final InAppNotificationRepository notifRepo;
    private final ReminderLogRepository logRepo;
    private final ZoneId zoneId;

    public ReminderScheduler(
            MeetingRepository meetingRepo,
            InAppNotificationRepository notifRepo,
            ReminderLogRepository logRepo,
            @Value("${taskchi.default-zone:Asia/Tehran}") String zone
    ) {
        this.meetingRepo = meetingRepo;
        this.notifRepo = notifRepo;
        this.logRepo = logRepo;
        this.zoneId = ZoneId.of(zone);
    }

    // every 60 seconds
    @Transactional
    @Scheduled(fixedDelay = 60_000)
    public void run() {
        Instant now = Instant.now();
        ZonedDateTime zNow = now.atZone(zoneId);

        // only look ahead 30 days for performance
        LocalDate from = zNow.toLocalDate().minusDays(1);
        LocalDate to = zNow.toLocalDate().plusDays(30);

        List<Meeting> meetings = meetingRepo.findByDateBetweenFetchAll(from, to);

        for (Meeting m : meetings) {
            // Ownership is required for in-app notifications
            if (m.getCreatedBy() == null) continue;
            ZonedDateTime meetingStart = toMeetingStart(m);

            // Respect per-meeting alarm toggle
            if (!m.isAlarmEnabled()) continue;

            // Mandatory same-day alert (at meeting start) + custom reminders
            java.util.LinkedHashSet<Integer> triggers = new java.util.LinkedHashSet<>();
            triggers.add(0);
            if (m.getReminderMinutesBefore() != null) triggers.addAll(m.getReminderMinutesBefore());

            for (Integer minutesBefore : triggers) {
                if (minutesBefore == null || minutesBefore < 0) continue;

                Instant reminderAt = meetingStart.minusMinutes(minutesBefore).toInstant();

                // Fire if due (or late due to downtime), but not more than 48h late.
                if (reminderAt.isAfter(now)) continue;
                if (reminderAt.isBefore(now.minus(Duration.ofHours(48)))) continue;

                if (logRepo.existsByMeetingIdAndMinutesBefore(m.getId(), minutesBefore)) continue;

                // create notification
                InAppNotification n = new InAppNotification();
                n.setType("MEETING_REMINDER");
                n.setMeetingId(m.getId());
                n.setUser(m.getCreatedBy());
                n.setTitle("یادآوری رویداد: " + m.getTitle());
                n.setMessage(buildMessage(m, meetingStart, minutesBefore));

                notifRepo.save(n);

                ReminderLog log = new ReminderLog();
                log.setMeetingId(m.getId());
                log.setMinutesBefore(minutesBefore);
                logRepo.save(log);
            }
        }
    }

    private ZonedDateTime toMeetingStart(Meeting m) {
        // If all-day or missing time -> 09:00
        LocalTime start = (m.isAllDay() || m.getStartTime() == null) ? LocalTime.of(9, 0) : m.getStartTime();
        return ZonedDateTime.of(m.getDate(), start, zoneId);
    }

    private String buildMessage(Meeting m, ZonedDateTime meetingStart, int minutesBefore) {
        String when = m.isAllDay() ? "تمام روز" : meetingStart.toLocalTime().toString();
        String dd = meetingStart.toLocalDate().toString();

        String beforeHuman;
        if (minutesBefore == 0) {
            beforeHuman = "یادآوری اجباری همان‌روز";
        } else if (minutesBefore % (24 * 60) == 0) {
            beforeHuman = (minutesBefore / (24 * 60)) + " روز قبل";
        } else if (minutesBefore % 60 == 0) {
            beforeHuman = (minutesBefore / 60) + " ساعت قبل";
        } else {
            beforeHuman = minutesBefore + " دقیقه قبل";
        }

        String loc = (m.getLocation() == null || m.getLocation().isBlank()) ? "" : (" • " + m.getLocation().trim());

        return "رویداد در تاریخ " + dd + " ساعت " + when + loc + "\n"
                + "تنظیم یادآوری: " + beforeHuman;
    }
}
