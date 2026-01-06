package com.taskchi.taskchi.meeting;

import com.taskchi.taskchi.auth.CurrentUser;
import com.taskchi.taskchi.users.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService service;
    private final CurrentUser currentUser;

    public MeetingController(MeetingService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<MeetingDto> range(@RequestParam String from, @RequestParam String to, Authentication auth) {
        User me = currentUser.requireUser(auth);
        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);

        List<Meeting> meetings = service.range(me, f, t);
        List<MeetingDto> out = new ArrayList<>();
        for (Meeting m : meetings) out.add(toDto(m));
        return out;
    }

    @GetMapping("/{id}")
    public MeetingDto get(@PathVariable Long id, Authentication auth) {
        User me = currentUser.requireUser(auth);
        return toDto(service.get(me, id));
    }

    @PostMapping
    public ResponseEntity<MeetingDto> create(@RequestBody MeetingDto dto, Authentication auth) {
        User me = currentUser.requireUser(auth);
        Meeting created = service.create(me, dto);
        return ResponseEntity.ok(toDto(created));
    }

    @PutMapping("/{id}")
    public MeetingDto update(@PathVariable Long id, @RequestBody MeetingDto dto, Authentication auth) {
        User me = currentUser.requireUser(auth);
        return toDto(service.update(me, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        User me = currentUser.requireUser(auth);
        service.delete(me, id);
        return ResponseEntity.noContent().build();
    }

    private static MeetingDto toDto(Meeting m) {
        MeetingDto dto = new MeetingDto();
        dto.id = m.getId();
        dto.title = m.getTitle();
        dto.date = m.getDate().toString();
        dto.allDay = m.isAllDay();
        dto.alarmEnabled = m.isAlarmEnabled();
        dto.startTime = m.getStartTime() == null ? null : m.getStartTime().toString();
        dto.endTime = m.getEndTime() == null ? null : m.getEndTime().toString();
        dto.location = m.getLocation();
        dto.content = m.getContent();
        dto.outcome = m.getOutcome();
        dto.reminderMinutesBefore = m.getReminderMinutesBefore();
        return dto;
    }
}
