// backend/src/main/java/com/taskchi/taskchi/meeting/MeetingService.java
package com.taskchi.taskchi.meeting;

import com.taskchi.taskchi.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class MeetingService {

    private final MeetingRepository repo;

    public MeetingService(MeetingRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Meeting> range(User owner, LocalDate from, LocalDate to) {
        if (owner == null) throw new IllegalArgumentException("owner is required");
        return repo.findByCreatedByIdInRangeWithReminders(owner.getId(), from, to);
    }

    @Transactional(readOnly = true)
    public Meeting get(User owner, Long id) {
        if (owner == null) throw new IllegalArgumentException("owner is required");
        return repo.findByIdAndCreatedByIdWithReminders(id, owner.getId())
                .orElseThrow(() -> new NoSuchElementException("Meeting not found: " + id));
    }

    @Transactional
    public Meeting create(User owner, MeetingDto dto) {
        if (owner == null) throw new IllegalArgumentException("owner is required");
        Meeting m = new Meeting();
        m.setCreatedBy(owner);
        applyDto(m, dto, true);
        return repo.save(m);
    }

    @Transactional
    public Meeting update(User owner, Long id, MeetingDto dto) {
        Meeting m = get(owner, id);
        applyDto(m, dto, false);
        return repo.save(m);
    }

    @Transactional
    public void delete(User owner, Long id) {
        Meeting m = get(owner, id);
        repo.delete(m);
    }

    private void applyDto(Meeting m, MeetingDto dto, boolean isCreate) {
        if (dto == null) throw new IllegalArgumentException("Body is required");

        String title = safeTrim(dto.title);
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");

        LocalDate date;
        try {
            date = LocalDate.parse(dto.date);
        } catch (Exception e) {
            throw new IllegalArgumentException("date must be yyyy-MM-dd");
        }

        boolean allDay = dto.allDay;

        // alarms/reminders toggle
        Boolean alarmEnabledRaw = dto.alarmEnabled;
        boolean alarmEnabled = isCreate
                ? (alarmEnabledRaw == null || alarmEnabledRaw)
                : (alarmEnabledRaw != null ? alarmEnabledRaw : m.isAlarmEnabled());

        LocalTime start = null;
        LocalTime end = null;

        if (!allDay) {
            if (dto.startTime == null || dto.startTime.isBlank()) {
                throw new IllegalArgumentException("startTime is required when allDay=false");
            }
            try {
                start = LocalTime.parse(dto.startTime);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("startTime must be HH:mm");
            }
            if (dto.endTime != null && !dto.endTime.isBlank()) {
                try {
                    end = LocalTime.parse(dto.endTime);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("endTime must be HH:mm");
                }
                if (end.isBefore(start) || end.equals(start)) {
                    throw new IllegalArgumentException("endTime must be after startTime");
                }
            }
        }

        List<Integer> reminders = normalizeReminders(dto.reminderMinutesBefore);

        m.setTitle(title);
        m.setDate(date);
        m.setAllDay(allDay);
        m.setAlarmEnabled(alarmEnabled);
        m.setStartTime(start);
        m.setEndTime(end);
        m.setLocation(safeTrim(dto.location));
        m.setContent(dto.content);
        m.setOutcome(dto.outcome);
        m.setReminderMinutesBefore(reminders);
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static List<Integer> normalizeReminders(List<Integer> input) {
        if (input == null) return new ArrayList<>();
        // Dedup + keep positive only
        TreeSet<Integer> set = new TreeSet<>(Collections.reverseOrder());
        for (Integer v : input) {
            if (v == null) continue;
            if (v <= 0) continue;
            // Hard safety cap: 365 days
            if (v > 365 * 24 * 60) continue;
            set.add(v);
        }
        return new ArrayList<>(set);
    }
}
