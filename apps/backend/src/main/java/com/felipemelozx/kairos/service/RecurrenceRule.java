package com.felipemelozx.kairos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipemelozx.kairos.dto.request.SeriesPatternRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Validated recurrence rule: parses/validates the series pattern JSON
 * (PRD 5.5 wire format) and expands it into occurrence dates.
 * Shared by {@link TimeBlockSeriesService} and {@link SeriesRollForwardService}.
 */
public final class RecurrenceRule {

    public enum Type {
        DAILY, DAILY_INTERVAL, WEEKLY_WEEKDAYS, SINGLE
    }

    public static final int MAX_OCCURRENCES = 366;

    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Type type;
    private final int interval;
    private final Set<Integer> weekdays;
    private final LocalTime time;
    private final int durationMinutes;

    private RecurrenceRule(Type type, int interval, Set<Integer> weekdays, LocalTime time, int durationMinutes) {
        this.type = type;
        this.interval = interval;
        this.weekdays = weekdays;
        this.time = time;
        this.durationMinutes = durationMinutes;
    }

    public static RecurrenceRule parse(SeriesPatternRequest request) {
        if (request == null || request.type() == null) {
            throw new IllegalArgumentException("Series pattern type is required");
        }
        Type type;
        try {
            type = Type.valueOf(request.type());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown series pattern type: " + request.type());
        }
        if (request.time() == null || !TIME_PATTERN.matcher(request.time()).matches()) {
            throw new IllegalArgumentException("Series pattern time must be HH:mm (00:00-23:59)");
        }
        if (request.durationMinutes() == null || request.durationMinutes() < 1 || request.durationMinutes() > 1440) {
            throw new IllegalArgumentException("Series pattern durationMinutes must be between 1 and 1440");
        }
        int interval = 1;
        if (type == Type.DAILY_INTERVAL) {
            interval = request.interval() == null ? 1 : request.interval();
            if (interval < 1) {
                throw new IllegalArgumentException("Series pattern interval must be >= 1");
            }
        }
        Set<Integer> weekdays = Set.of();
        if (type == Type.WEEKLY_WEEKDAYS) {
            if (request.weekdays() == null || request.weekdays().isEmpty()) {
                throw new IllegalArgumentException("Series pattern weekdays must be a non-empty subset of 0-6");
            }
            for (Integer day : request.weekdays()) {
                if (day == null || day < 0 || day > 6) {
                    throw new IllegalArgumentException("Series pattern weekdays must be a non-empty subset of 0-6");
                }
            }
            weekdays = new HashSet<>(request.weekdays());
        }
        return new RecurrenceRule(type, interval, weekdays,
                LocalTime.parse(request.time()), request.durationMinutes());
    }

    public static RecurrenceRule parseJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Series pattern must not be blank");
        }
        try {
            return parse(MAPPER.readValue(json, SeriesPatternRequest.class));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Series pattern is not valid JSON: " + ex.getMessage());
        }
    }

    public static String toJson(SeriesPatternRequest request) {
        RecurrenceRule rule = parse(request);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", rule.type.name());
        if (rule.type == Type.DAILY_INTERVAL) {
            map.put("interval", rule.interval);
        }
        if (rule.type == Type.WEEKLY_WEEKDAYS) {
            map.put("weekdays", new ArrayList<>(rule.weekdays).stream().sorted().toList());
        }
        map.put("time", rule.time.toString());
        map.put("durationMinutes", rule.durationMinutes);
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize series pattern", ex);
        }
    }

    public List<LocalDate> datesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        if (from == null || to == null || from.isAfter(to)) {
            return dates;
        }
        if (type == Type.SINGLE) {
            dates.add(from);
            return dates;
        }
        LocalDate current = from;
        while (!current.isAfter(to) && dates.size() < MAX_OCCURRENCES) {
            if (matches(current, from)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    private boolean matches(LocalDate date, LocalDate start) {
        return switch (type) {
            case DAILY -> true;
            case DAILY_INTERVAL -> ChronoUnit.DAYS.between(start, date) % interval == 0;
            case WEEKLY_WEEKDAYS -> weekdays.contains(date.getDayOfWeek().getValue() % 7);
            case SINGLE -> date.equals(start);
        };
    }

    public Instant startInstant(LocalDate date) {
        return date.atTime(time).atZone(ZoneOffset.UTC).toInstant();
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Type getType() {
        return type;
    }
}
