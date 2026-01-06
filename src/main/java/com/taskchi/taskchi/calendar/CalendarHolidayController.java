package com.taskchi.taskchi.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.PersianCalendar;
import com.ibm.icu.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/calendar")
public class CalendarHolidayController {
    private static final Logger log = LoggerFactory.getLogger(CalendarHolidayController.class);

    private final RestClient client;
    private final Map<String, List<HolidayDto>> cache = new ConcurrentHashMap<>();

    public CalendarHolidayController(RestClient.Builder builder) {
        // Source: holidayapi.ir (data extracted from time.ir)
        this.client = builder.baseUrl("https://holidayapi.ir").build();
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
        // NOTE: holidayapi.ir doesn't provide a range endpoint. We'll iterate Jalali days and query per-day endpoint.
        try {
            JalaliDate s = parseDayId(start);
            JalaliDate e = parseDayId(end);

            PersianCalendar cur = new PersianCalendar();
            cur.setTimeZone(TimeZone.getTimeZone("UTC")); // ✅ ICU TimeZone
            cur.set(PersianCalendar.YEAR, s.y);
            cur.set(PersianCalendar.MONTH, s.m - 1);
            cur.set(PersianCalendar.DAY_OF_MONTH, s.d);
            cur.set(PersianCalendar.HOUR_OF_DAY, 0);
            cur.set(PersianCalendar.MINUTE, 0);
            cur.set(PersianCalendar.SECOND, 0);
            cur.set(PersianCalendar.MILLISECOND, 0);

            PersianCalendar endCal = new PersianCalendar();
            endCal.setTimeZone(TimeZone.getTimeZone("UTC")); // ✅ ICU TimeZone
            endCal.set(PersianCalendar.YEAR, e.y);
            endCal.set(PersianCalendar.MONTH, e.m - 1);
            endCal.set(PersianCalendar.DAY_OF_MONTH, e.d);
            endCal.set(PersianCalendar.HOUR_OF_DAY, 0);
            endCal.set(PersianCalendar.MINUTE, 0);
            endCal.set(PersianCalendar.SECOND, 0);
            endCal.set(PersianCalendar.MILLISECOND, 0);

            long endMillis = endCal.getTimeInMillis();

            List<HolidayDto> out = new ArrayList<>();
            while (cur.getTimeInMillis() <= endMillis) {
                int y = cur.get(PersianCalendar.YEAR);
                int m = cur.get(PersianCalendar.MONTH) + 1;
                int d = cur.get(PersianCalendar.DAY_OF_MONTH);

                String dayId = formatDayId(y, m, d);
                HolidayDto dto = fetchDay(y, m, d, dayId);
                if (dto != null && dto.holiday) {
                    out.add(dto);
                }

                cur.add(Calendar.DAY_OF_MONTH, 1); // ✅ ICU Calendar constant
            }

            return out;
        } catch (Exception ex) {
            log.warn("Failed to fetch holidays {}..{}: {}", start, end, ex.toString());
            return List.of(); // فallback: UI فقط جمعه‌ها رو رنگ می‌کنه
        }
    }

    private HolidayDto fetchDay(int y, int m, int d, String dayId) {
        try {
            String mm = String.format("%02d", m);
            String dd = String.format("%02d", d);

            // GET https://holidayapi.ir/jalali/{year}/{month}/{day}
            HolidayApiResponse resp = client.get()
                    .uri("/jalali/{y}/{m}/{d}", y, mm, dd)
                    .retrieve()
                    .body(HolidayApiResponse.class);

            if (resp == null || resp.events == null) return null;

            // IMPORTANT:
            // holidayapi.ir marks ALL Fridays as is_holiday=true because "جمعه" is a holiday event.
            // We only want to treat a day as "official holiday" when there is a holiday event OTHER THAN "جمعه".
            List<String> events = new ArrayList<>();
            List<String> holidayEvents = new ArrayList<>();

            for (HolidayApiEvent ev : resp.events) {
                if (ev == null) continue;
                String desc = safe(ev.description);
                if (desc.isBlank()) continue;

                if (!"جمعه".equals(desc)) {
                    events.add(desc);
                }

                if (ev.isHoliday && !"جمعه".equals(desc)) {
                    holidayEvents.add(desc);
                }
            }

            boolean isOfficialHoliday = !holidayEvents.isEmpty();
            if (!isOfficialHoliday) return null;

            String cause = holidayEvents.getFirst(); // Java 21 OK
            return new HolidayDto(dayId, true, cause, events);

        } catch (Exception ex) {
            // If API fails for this day, just skip.
            return null;
        }
    }

    private static void validate(String yyyymmdd) {
        if (yyyymmdd == null || !yyyymmdd.matches("^\\d{8}$")) {
            throw new IllegalArgumentException("Invalid date format. expected YYYYMMDD (Jalali)");
        }
    }

    private static JalaliDate parseDayId(String dayId) {
        int y = Integer.parseInt(dayId.substring(0, 4));
        int m = Integer.parseInt(dayId.substring(4, 6));
        int d = Integer.parseInt(dayId.substring(6, 8));
        if (m < 1 || m > 12) throw new IllegalArgumentException("Invalid month in dayId: " + dayId);
        if (d < 1 || d > 31) throw new IllegalArgumentException("Invalid day in dayId: " + dayId);
        return new JalaliDate(y, m, d);
    }

    private static String formatDayId(int y, int m, int d) {
        return String.format("%04d%02d%02d", y, m, d);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public record HolidayDto(String dayId, boolean holiday, String cause, List<String> events) {}

    private record JalaliDate(int y, int m, int d) {}

    // Response schema of holidayapi.ir
    public static class HolidayApiResponse {
        @JsonProperty("is_holiday")
        public boolean isHoliday;

        public List<HolidayApiEvent> events;
    }

    public static class HolidayApiEvent {
        public String description;

        @JsonProperty("additional_description")
        public String additionalDescription;

        @JsonProperty("is_holiday")
        public boolean isHoliday;

        @JsonProperty("is_religious")
        public boolean isReligious;
    }
}
