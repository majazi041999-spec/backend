package com.taskchi.taskchi.meeting;

import java.util.List;

public class MeetingDto {
    public Long id;

    public String title;           // required
    public String date;            // yyyy-MM-dd required
    public String startTime;       // HH:mm optional
    public String endTime;         // HH:mm optional
    public boolean allDay;

    public Boolean alarmEnabled; // optional; default true

    public String location;
    public String content;
    public String outcome;

    public List<Integer> reminderMinutesBefore; // e.g. [2880, 1440, 120]
}
