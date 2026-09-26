package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.entity.TimeBlock;
import com.felipemelozx.kairos.entity.TimeBlockSeries;
import com.felipemelozx.kairos.repository.TimeBlockRepository;
import com.felipemelozx.kairos.repository.TimeBlockSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeriesRollForwardServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-25T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private TimeBlockSeriesRepository seriesRepository;

    @Mock
    private TimeBlockRepository timeBlockRepository;

    private SeriesRollForwardService rollForwardService;

    @BeforeEach
    void setUp() {
        rollForwardService = new SeriesRollForwardService(seriesRepository, timeBlockRepository, FIXED_CLOCK);
    }

    @Test
    void shouldExtendIndefiniteSeriesSkippingExisting() {
        TimeBlockSeries series = indefiniteSeries("DAILY", LocalDate.parse("2026-09-01"));
        TimeBlock existing = storedBlock(series, Instant.parse("2026-09-01T09:00:00Z"));
        when(seriesRepository.findActiveForRollForward(LocalDate.parse("2026-09-25")))
                .thenReturn(List.of(series));
        when(seriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(timeBlockRepository.findBySeriesIdAndUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(
                series.getId(), series.getUserId())).thenReturn(List.of(existing));

        int created = rollForwardService.extendIndefiniteSeries();

        // Incremental: window starts 2026-09-02, horizon 2027-09-25 -> capped at 366 new.
        assertThat(created).isEqualTo(366);
        ArgumentCaptor<List<TimeBlock>> blocks = ArgumentCaptor.forClass(List.class);
        verify(timeBlockRepository).saveAll(blocks.capture());
        assertThat(blocks.getValue()).hasSize(366);
        assertThat(blocks.getValue().get(0).getStartDateTime()).isEqualTo(Instant.parse("2026-09-02T09:00:00Z"));
        assertThat(blocks.getValue()).allMatch(b -> b.getSeriesId().equals(series.getId()));
        assertThat(blocks.getValue()).allMatch(b -> !b.isOverride());
    }

    @Test
    void shouldResumeOldSeriesNearHorizon() {
        // FIX-002 regression: series older than 366 days must still reach the horizon.
        TimeBlockSeries series = indefiniteSeries("DAILY", LocalDate.parse("2024-01-01"));
        TimeBlock last = storedBlock(series, Instant.parse("2026-09-20T09:00:00Z"));
        when(seriesRepository.findActiveForRollForward(LocalDate.parse("2026-09-25")))
                .thenReturn(List.of(series));
        when(seriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(timeBlockRepository.findBySeriesIdAndUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(
                series.getId(), series.getUserId())).thenReturn(List.of(last));

        int created = rollForwardService.extendIndefiniteSeries();

        assertThat(created).isPositive();
        ArgumentCaptor<List<TimeBlock>> blocks = ArgumentCaptor.forClass(List.class);
        verify(timeBlockRepository).saveAll(blocks.capture());
        assertThat(blocks.getValue().get(0).getStartDateTime())
                .isEqualTo(Instant.parse("2026-09-21T09:00:00Z"));
    }

    @Test
    void shouldDoNothingWhenFullyMaterialized() {
        TimeBlockSeries series = indefiniteSeries("SINGLE", LocalDate.parse("2026-09-01"));
        TimeBlock existing = storedBlock(series, Instant.parse("2026-09-01T09:00:00Z"));
        when(seriesRepository.findActiveForRollForward(LocalDate.parse("2026-09-25")))
                .thenReturn(List.of(series));
        when(seriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        int created = rollForwardService.extendIndefiniteSeries();

        assertThat(created).isZero();
        verify(timeBlockRepository, never()).saveAll(any());
    }

    @Test
    void shouldSkipSeriesWithInvalidPattern() {
        TimeBlockSeries series = indefiniteSeries("DAILY", LocalDate.parse("2026-09-01"));
        series.setPattern("broken");
        when(seriesRepository.findActiveForRollForward(LocalDate.parse("2026-09-25")))
                .thenReturn(List.of(series));
        when(seriesRepository.findById(series.getId())).thenReturn(Optional.of(series));

        int created = rollForwardService.extendIndefiniteSeries();

        assertThat(created).isZero();
        verify(timeBlockRepository, never()).saveAll(any());
    }

    private TimeBlockSeries indefiniteSeries(String type, LocalDate start) {
        TimeBlockSeries series = new TimeBlockSeries();
        series.setId(UUID.randomUUID());
        series.setUserId(UUID.randomUUID());
        series.setTitle("Ingles");
        series.setStartDate(start);
        series.setEndDate(null);
        series.setPattern("{\"type\":\"" + type + "\",\"time\":\"09:00\",\"durationMinutes\":60}");
        series.setCreatedAt(FIXED_NOW);
        return series;
    }

    private TimeBlock storedBlock(TimeBlockSeries series, Instant start) {
        TimeBlock block = new TimeBlock();
        block.setId(UUID.randomUUID());
        block.setUserId(series.getUserId());
        block.setTitle(series.getTitle());
        block.setStartDateTime(start);
        block.setEndDateTime(start.plusSeconds(3600));
        block.setSeriesId(series.getId());
        block.setOverride(false);
        block.setCreatedAt(FIXED_NOW);
        return block;
    }
}
