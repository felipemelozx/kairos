package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.dto.request.SeriesPatternRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceRuleTest {

    @Test
    void shouldExpandDailyBetweenDates() {
        RecurrenceRule rule = RecurrenceRule.parse(
                new SeriesPatternRequest("DAILY", null, null, "09:00", 60));

        List<LocalDate> dates = rule.datesBetween(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"));

        assertThat(dates).containsExactly(
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-03"));
    }

    @Test
    void shouldExpandDailyIntervalEveryTwoDays() {
        RecurrenceRule rule = RecurrenceRule.parse(
                new SeriesPatternRequest("DAILY_INTERVAL", 2, null, "09:00", 60));

        List<LocalDate> dates = rule.datesBetween(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-05"));

        assertThat(dates).containsExactly(
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-05"));
    }

    @Test
    void shouldExpandWeeklyWeekdaysMatchingJsGetDay() {
        // 2026-10-05 is a Monday; weekdays [1, 3] = Mon/Wed
        RecurrenceRule rule = RecurrenceRule.parse(
                new SeriesPatternRequest("WEEKLY_WEEKDAYS", null, List.of(1, 3), "09:00", 60));

        List<LocalDate> dates = rule.datesBetween(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-11"));

        assertThat(dates).containsExactly(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-07"));
    }

    @Test
    void shouldCapOccurrencesAt366() {
        RecurrenceRule rule = RecurrenceRule.parse(
                new SeriesPatternRequest("DAILY", null, null, "09:00", 60));

        List<LocalDate> dates = rule.datesBetween(LocalDate.parse("2026-01-01"), LocalDate.parse("2028-01-01"));

        assertThat(dates).hasSize(RecurrenceRule.MAX_OCCURRENCES);
    }

    @Test
    void shouldRoundTripPatternJson() {
        SeriesPatternRequest request = new SeriesPatternRequest("WEEKLY_WEEKDAYS", null, List.of(3, 1), "09:00", 60);

        RecurrenceRule rule = RecurrenceRule.parseJson(RecurrenceRule.toJson(request));

        assertThat(rule.getType()).isEqualTo(RecurrenceRule.Type.WEEKLY_WEEKDAYS);
    }

    @Test
    void shouldRejectUnknownType() {
        assertThatThrownBy(() -> RecurrenceRule.parse(
                new SeriesPatternRequest("MONTHLY", null, null, "09:00", 60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectZeroInterval() {
        assertThatThrownBy(() -> RecurrenceRule.parse(
                new SeriesPatternRequest("DAILY_INTERVAL", 0, null, "09:00", 60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectWeekdayOutOfRange() {
        assertThatThrownBy(() -> RecurrenceRule.parse(
                new SeriesPatternRequest("WEEKLY_WEEKDAYS", null, List.of(7), "09:00", 60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankJson() {
        assertThatThrownBy(() -> RecurrenceRule.parseJson("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
