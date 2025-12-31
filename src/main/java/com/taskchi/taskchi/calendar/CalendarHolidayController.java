package com.taskchi.taskchi.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/calendar")
public class CalendarHolidayController {
    private static final Logger log = LoggerFactory.getLogger(CalendarHolidayController.class);

    private final RestClient client;
    private final Map<String, List<HolidayDto>> cache = new ConcurrentHashMap<>();

    public CalendarHolidayController(RestClient.Builder builder) {
        this.client = builder.baseUrl("https://persianholiday.site").build();
    }

    // مثال: /api/calendar/holidays/range?start=14040101&end=14040131
    @GetMapping("/holidays/range")
    public List<HolidayDto> range(@RequestParam("start") String start,
                                  @RequestParam("end") String end) {

        validate(start);
        validate(end);

        String key = start + "_" + end;
        return cache.computeIfAbsent(key, k -> fetch(start, end));
    }

    private List<HolidayDto> fetch(String start, String end) {
        try {
            RemoteHoliday[] arr = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/range")
                            .queryParam("start_date", start)
                            .queryParam("end_date", end)
                            .build())
                    .retrieve()
                    .body(RemoteHoliday[].class);

            if (arr == null) return List.of();

            List<HolidayDto> out = new ArrayList<>();
            for (RemoteHoliday r : arr) {
                if (r != null && r.holiday) {
                    out.add(new HolidayDto(
                            r.dayId,
                            true,
                            r.cause,
                            r.events == null ? List.of() : r.events
                    ));
                }
            }
            return out;
        } catch (Exception ex) {
            log.warn("Failed to fetch holidays {}..{}: {}", start, end, ex.toString());
            return List.of(); // فallback: UI فقط جمعه‌ها رو رنگ می‌کنه
        }
    }

    private static void validate(String yyyymmdd) {
        if (yyyymmdd == null || !yyyymmdd.matches("^\\d{8}$")) {
            throw new IllegalArgumentException("Invalid date format. expected YYYYMMDD (Jalali)");
        }
    }

    public record HolidayDto(String dayId, boolean holiday, String cause, List<String> events) {}

    // ساختار ریسپانس سایت
    public static class RemoteHoliday {
        @JsonProperty("day_id")
        public String dayId;

        public boolean holiday;
        public String cause;
        public List<String> events;
    }
}