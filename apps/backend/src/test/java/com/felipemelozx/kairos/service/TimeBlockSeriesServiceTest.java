package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.common.AppError;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.CreateSeriesRequest;
import com.felipemelozx.kairos.dto.request.SeriesPatternRequest;
import com.felipemelozx.kairos.dto.request.UpdateSeriesRequest;
import com.felipemelozx.kairos.dto.response.SeriesResponse;
import com.felipemelozx.kairos.entity.TimeBlock;
import com.felipemelozx.kairos.entity.TimeBlockSeries;
import com.felipemelozx.kairos.repository.ProjectRepository;
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
class TimeBlockSeriesServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-25T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private TimeBlockSeriesRepository seriesRepository;

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private ProjectRepository projectRepository;

    private TimeBlockSeriesService seriesService;

    @BeforeEach
    void setUp() {
        seriesService = new TimeBlockSeriesService(seriesRepository, timeBlockRepository, projectRepository, FIXED_CLOCK);
    }

    @Test
    void shouldCreateDailySeriesAndMaterializeOccurrences() {
        UUID userId = UUID.randomUUID();
        CreateSeriesRequest request = new CreateSeriesRequest("Ingles", null,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"),
                new SeriesPatternRequest("DAILY", null, null, "09:00", 60));
        when(seriesRepository.save(any(TimeBlockSeries.class))).thenAnswer(invocation -> {
            TimeBlockSeries series = invocation.getArgument(0);
            series.setId(UUID.randomUUID());
            return series;
        });
        when(timeBlockRepository.saveAll(any())).thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));

        SeriesResponse result = assertOk(seriesService.create(userId, request));

        assertThat(result.title()).isEqualTo("Ingles");
        assertThat(result.userId()).isEqualTo(userId);
        ArgumentCaptor<List<TimeBlock>> blocks = ArgumentCaptor.forClass(List.class);
        verify(timeBlockRepository).saveAll(blocks.capture());
        assertThat(blocks.getValue()).hasSize(3);
        TimeBlock first = blocks.getValue().get(0);
        assertThat(first.getStartDateTime()).isEqualTo(Instant.parse("2026-10-01T09:00:00Z"));
        assertThat(first.getEndDateTime()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(first.getSeriesId()).isEqualTo(result.id());
        assertThat(first.isOverride()).isFalse();
    }

    @Test
    void shouldRejectEmptyWeekdays() {
        UUID userId = UUID.randomUUID();
        CreateSeriesRequest request = new CreateSeriesRequest("Ingles", null,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"),
                new SeriesPatternRequest("WEEKLY_WEEKDAYS", null, List.of(), "09:00", 60));

        AppError error = assertErr(seriesService.create(userId, request));

        assertThat(error.code()).isEqualTo("INVALID_SERIES_PATTERN");
        verify(seriesRepository, never()).save(any(TimeBlockSeries.class));
        verify(timeBlockRepository, never()).saveAll(any());
    }

    @Test
    void shouldRejectEndBeforeStart() {
        UUID userId = UUID.randomUUID();
        CreateSeriesRequest request = new CreateSeriesRequest("Ingles", null,
                LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-01"),
                new SeriesPatternRequest("DAILY", null, null, "09:00", 60));

        AppError error = assertErr(seriesService.create(userId, request));

        assertThat(error.code()).isEqualTo("INVALID_SERIES_RANGE");
        verify(seriesRepository, never()).save(any(TimeBlockSeries.class));
    }

    @Test
    void shouldRejectBadTimeFormat() {
        UUID userId = UUID.randomUUID();
        CreateSeriesRequest request = new CreateSeriesRequest("Ingles", null,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"),
                new SeriesPatternRequest("DAILY", null, null, "25:00", 60));

        AppError error = assertErr(seriesService.create(userId, request));

        assertThat(error.code()).isEqualTo("INVALID_SERIES_PATTERN");
        verify(seriesRepository, never()).save(any(TimeBlockSeries.class));
    }

    @Test
    void shouldRejectBadDuration() {
        UUID userId = UUID.randomUUID();
        CreateSeriesRequest request = new CreateSeriesRequest("Ingles", null,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"),
                new SeriesPatternRequest("DAILY", null, null, "09:00", 0));

        AppError error = assertErr(seriesService.create(userId, request));

        assertThat(error.code()).isEqualTo("INVALID_SERIES_PATTERN");
        verify(seriesRepository, never()).save(any(TimeBlockSeries.class));
    }

    @Test
    void shouldSplitSeriesPreservingPast() {
        UUID userId = UUID.randomUUID();
        TimeBlockSeries old = storedSeries(userId, "Ingles", LocalDate.parse("2026-09-01"), null, "09:00", 60);
        when(seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(old.getId(), userId))
                .thenReturn(Optional.of(old));
        when(seriesRepository.save(any(TimeBlockSeries.class))).thenAnswer(invocation -> {
            TimeBlockSeries series = invocation.getArgument(0);
            if (series.getId() == null) {
                series.setId(UUID.randomUUID());
            }
            return series;
        });
        when(timeBlockRepository.saveAll(any())).thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));
        UpdateSeriesRequest request = new UpdateSeriesRequest(LocalDate.parse("2026-10-01"), null, null, null,
                new SeriesPatternRequest("DAILY", null, null, "10:00", 30));

        SeriesResponse result = assertOk(seriesService.update(old.getId(), userId, request));

        assertThat(old.getEndDate()).isEqualTo(LocalDate.parse("2026-09-30"));
        assertThat(result.startDate()).isEqualTo(LocalDate.parse("2026-10-01"));
        verify(timeBlockRepository).softDeleteFutureBySeriesId(
                old.getId(), userId, Instant.parse("2026-10-01T00:00:00Z"), FIXED_NOW);
        ArgumentCaptor<List<TimeBlock>> blocks = ArgumentCaptor.forClass(List.class);
        verify(timeBlockRepository).saveAll(blocks.capture());
        assertThat(blocks.getValue()).isNotEmpty();
        assertThat(blocks.getValue()).allMatch(b -> !b.getStartDateTime().isBefore(Instant.parse("2026-10-01T00:00:00Z")));
        assertThat(blocks.getValue().get(0).getStartDateTime()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
    }

    @Test
    void shouldRejectSplitWhenEffectiveFromNotAfterStart() {
        UUID userId = UUID.randomUUID();
        TimeBlockSeries old = storedSeries(userId, "Ingles", LocalDate.parse("2026-09-01"), null, "09:00", 60);
        when(seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(old.getId(), userId))
                .thenReturn(Optional.of(old));
        UpdateSeriesRequest request = new UpdateSeriesRequest(LocalDate.parse("2026-09-01"), null, null, null,
                new SeriesPatternRequest("DAILY", null, null, "10:00", 30));

        AppError error = assertErr(seriesService.update(old.getId(), userId, request));

        assertThat(error.code()).isEqualTo("INVALID_SERIES_RANGE");
        verify(timeBlockRepository, never()).saveAll(any());
    }

    @Test
    void shouldDeleteFutureOccurrencesPreservingPast() {
        UUID userId = UUID.randomUUID();
        TimeBlockSeries series = storedSeries(userId, "Ingles", LocalDate.parse("2026-09-01"), null, "09:00", 60);
        when(seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(series.getId(), userId))
                .thenReturn(Optional.of(series));

        Result<Void> result = seriesService.softDelete(series.getId(), userId);

        assertThat(result.isOk()).isTrue();
        verify(seriesRepository).softDeleteByIdAndUserId(series.getId(), userId, FIXED_NOW);
        verify(timeBlockRepository).softDeleteFutureBySeriesId(
                series.getId(), userId, FIXED_NOW, FIXED_NOW);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingOtherUsersSeries() {
        UUID userB = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        when(seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(seriesId, userB))
                .thenReturn(Optional.empty());

        AppError error = assertErr(seriesService.softDelete(seriesId, userB));

        assertThat(error.code()).isEqualTo("NOT_FOUND");
        verify(seriesRepository, never()).softDeleteByIdAndUserId(any(), any(), any());
        verify(timeBlockRepository, never()).softDeleteFutureBySeriesId(any(), any(), any(), any());
    }

    @Test
    void shouldAllowOverlappingSeries() {
        UUID userId = UUID.randomUUID();
        CreateSeriesRequest first = new CreateSeriesRequest("A", null,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02"),
                new SeriesPatternRequest("DAILY", null, null, "09:00", 60));
        CreateSeriesRequest overlap = new CreateSeriesRequest("B", null,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02"),
                new SeriesPatternRequest("DAILY", null, null, "09:30", 60));
        when(seriesRepository.save(any(TimeBlockSeries.class))).thenAnswer(invocation -> {
            TimeBlockSeries series = invocation.getArgument(0);
            series.setId(UUID.randomUUID());
            return series;
        });
        when(timeBlockRepository.saveAll(any())).thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));

        assertOk(seriesService.create(userId, first));
        SeriesResponse result = assertOk(seriesService.create(userId, overlap));

        assertThat(result.title()).isEqualTo("B");
    }

    @Test
    void shouldRejectForeignProject() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId))
                .thenReturn(Optional.empty());
        CreateSeriesRequest request = new CreateSeriesRequest("Ingles", projectId,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-03"),
                new SeriesPatternRequest("DAILY", null, null, "09:00", 60));

        AppError error = assertErr(seriesService.create(userId, request));

        assertThat(error.code()).isEqualTo("NOT_FOUND");
        verify(seriesRepository, never()).save(any(TimeBlockSeries.class));
    }

    @Test
    void shouldCreateSingleSeriesWithExactlyOneOccurrence() {
        UUID userId = UUID.randomUUID();
        CreateSeriesRequest request = new CreateSeriesRequest("Interview", null,
                LocalDate.parse("2026-10-07"), null,
                new SeriesPatternRequest("SINGLE", null, null, "14:00", 90));
        when(seriesRepository.save(any(TimeBlockSeries.class))).thenAnswer(invocation -> {
            TimeBlockSeries series = invocation.getArgument(0);
            series.setId(UUID.randomUUID());
            return series;
        });
        when(timeBlockRepository.saveAll(any())).thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));

        assertOk(seriesService.create(userId, request));

        ArgumentCaptor<List<TimeBlock>> blocks = ArgumentCaptor.forClass(List.class);
        verify(timeBlockRepository).saveAll(blocks.capture());
        assertThat(blocks.getValue()).hasSize(1);
        assertThat(blocks.getValue().get(0).getStartDateTime()).isEqualTo(Instant.parse("2026-10-07T14:00:00Z"));
    }

    private TimeBlockSeries storedSeries(UUID userId, String title, LocalDate start, LocalDate end,
            String time, int duration) {
        TimeBlockSeries series = new TimeBlockSeries();
        series.setId(UUID.randomUUID());
        series.setUserId(userId);
        series.setTitle(title);
        series.setStartDate(start);
        series.setEndDate(end);
        series.setPattern("{\"type\":\"DAILY\",\"time\":\"" + time + "\",\"durationMinutes\":" + duration + "}");
        series.setCreatedAt(FIXED_NOW);
        return series;
    }

    private static <T> T assertOk(Result<T> result) {
        assertThat(result.isOk()).isTrue();
        return result.fold(value -> value, err -> {
            throw new AssertionError("Expected Ok but was Err: " + err.code());
        });
    }

    private static <T> AppError assertErr(Result<T> result) {
        assertThat(result.isErr()).isTrue();
        return result.fold(value -> {
            throw new AssertionError("Expected Err but was Ok");
        }, err -> err);
    }
}
