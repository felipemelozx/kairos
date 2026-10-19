package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.common.ErrorCode;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.CreateSeriesRequest;
import com.felipemelozx.kairos.dto.request.UpdateSeriesRequest;
import com.felipemelozx.kairos.dto.response.SeriesResponse;
import com.felipemelozx.kairos.entity.TimeBlock;
import com.felipemelozx.kairos.entity.TimeBlockSeries;
import com.felipemelozx.kairos.repository.ProjectRepository;
import com.felipemelozx.kairos.repository.TimeBlockRepository;
import com.felipemelozx.kairos.repository.TimeBlockSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TimeBlockSeriesService {

    private static final Logger log = LoggerFactory.getLogger(TimeBlockSeriesService.class);
    private static final int MAX_TITLE_LENGTH = 200;

    private final TimeBlockSeriesRepository seriesRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    @Autowired
    public TimeBlockSeriesService(TimeBlockSeriesRepository seriesRepository,
            TimeBlockRepository timeBlockRepository,
            ProjectRepository projectRepository) {
        this(seriesRepository, timeBlockRepository, projectRepository, Clock.systemUTC());
    }

    public TimeBlockSeriesService(TimeBlockSeriesRepository seriesRepository,
            TimeBlockRepository timeBlockRepository,
            ProjectRepository projectRepository,
            Clock clock) {
        this.seriesRepository = seriesRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Transactional
    public Result<SeriesResponse> create(UUID userId, CreateSeriesRequest request) {
        log.info("Creating time block series for userId={}", userId);
        Result<Void> titleCheck = validateTitle(request.title());
        if (titleCheck instanceof Result.Err<Void> err) {
            return Result.err(err.error());
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            return Result.err(ErrorCode.INVALID_SERIES_RANGE);
        }
        RecurrenceRule rule;
        try {
            rule = RecurrenceRule.parse(request.pattern());
        } catch (IllegalArgumentException ex) {
            return Result.err(ErrorCode.INVALID_SERIES_PATTERN, ex.getMessage());
        }
        if (request.projectId() != null) {
            Result<Void> projectCheck = validateProject(userId, request.projectId());
            if (projectCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
        }

        String patternJson = RecurrenceRule.toJson(request.pattern());
        TimeBlockSeries series = new TimeBlockSeries();
        series.setUserId(userId);
        series.setTitle(request.title());
        series.setProjectId(request.projectId());
        series.setStartDate(request.startDate());
        series.setEndDate(request.endDate());
        series.setPattern(patternJson);
        series.setCreatedAt(Instant.now(clock));

        TimeBlockSeries saved = seriesRepository.save(series);
        List<TimeBlock> occurrences = buildOccurrences(userId, saved, rule,
                request.startDate(), horizon(request.endDate()));
        timeBlockRepository.saveAll(occurrences);
        log.info("Time block series created: id={}, userId={}, occurrences={}", saved.getId(), userId,
                occurrences.size());
        return Result.ok(SeriesResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public Result<List<SeriesResponse>> listByUser(UUID userId) {
        log.info("Listing time block series for userId={}", userId);
        List<SeriesResponse> result = seriesRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateAsc(userId)
                .stream()
                .map(SeriesResponse::from)
                .toList();
        log.info("Listed {} time block series for userId={}", result.size(), userId);
        return Result.ok(result);
    }

    @Transactional(readOnly = true)
    public Result<SeriesResponse> getById(UUID id, UUID userId) {
        log.info("Fetching time block series: id={}, userId={}", id, userId);
        return seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .map(series -> Result.<SeriesResponse>ok(SeriesResponse.from(series)))
                .orElseGet(() -> Result.err(ErrorCode.NOT_FOUND, "Time block series not found"));
    }

    @Transactional
    public Result<SeriesResponse> update(UUID id, UUID userId, UpdateSeriesRequest request) {
        log.info("Splitting time block series: id={}, userId={}, effectiveFrom={}", id, userId,
                request.effectiveFrom());
        var maybeSeries = seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
        if (maybeSeries.isEmpty()) {
            return Result.err(ErrorCode.NOT_FOUND, "Time block series not found");
        }
        TimeBlockSeries old = maybeSeries.get();
        if (request.effectiveFrom() == null || !request.effectiveFrom().isAfter(old.getStartDate())) {
            return Result.err(ErrorCode.INVALID_SERIES_RANGE,
                    "effectiveFrom must be after the series start date");
        }

        String title = request.title() != null ? request.title() : old.getTitle();
        Result<Void> titleCheck = validateTitle(title);
        if (titleCheck instanceof Result.Err<Void> err) {
            return Result.err(err.error());
        }
        UUID projectId = request.projectId() != null ? request.projectId() : old.getProjectId();
        if (request.projectId() != null) {
            Result<Void> projectCheck = validateProject(userId, request.projectId());
            if (projectCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
        }
        LocalDate endDate = request.endDate() != null ? request.endDate() : old.getEndDate();
        if (endDate != null && endDate.isBefore(request.effectiveFrom())) {
            return Result.err(ErrorCode.INVALID_SERIES_RANGE);
        }
        RecurrenceRule rule;
        String patternJson;
        if (request.pattern() != null) {
            try {
                rule = RecurrenceRule.parse(request.pattern());
            } catch (IllegalArgumentException ex) {
                return Result.err(ErrorCode.INVALID_SERIES_PATTERN, ex.getMessage());
            }
            patternJson = RecurrenceRule.toJson(request.pattern());
        } else {
            try {
                rule = RecurrenceRule.parseJson(old.getPattern());
            } catch (IllegalArgumentException ex) {
                return Result.err(ErrorCode.INVALID_SERIES_PATTERN, ex.getMessage());
            }
            patternJson = old.getPattern();
        }

        old.setEndDate(request.effectiveFrom().minusDays(1));
        seriesRepository.save(old);
        Instant splitPoint = request.effectiveFrom().atStartOfDay().atZone(ZoneOffset.UTC).toInstant();
        timeBlockRepository.softDeleteFutureBySeriesId(old.getId(), userId, splitPoint, Instant.now(clock));

        TimeBlockSeries next = new TimeBlockSeries();
        next.setUserId(userId);
        next.setTitle(title);
        next.setProjectId(projectId);
        next.setStartDate(request.effectiveFrom());
        next.setEndDate(endDate);
        next.setPattern(patternJson);
        next.setCreatedAt(Instant.now(clock));
        TimeBlockSeries saved = seriesRepository.save(next);
        List<TimeBlock> occurrences = buildOccurrences(userId, saved, rule,
                request.effectiveFrom(), horizon(endDate));
        timeBlockRepository.saveAll(occurrences);
        log.info("Time block series split: oldId={}, newId={}, userId={}, occurrences={}", old.getId(),
                saved.getId(), userId, occurrences.size());
        return Result.ok(SeriesResponse.from(saved));
    }

    @Transactional
    public Result<Void> softDelete(UUID id, UUID userId) {
        log.info("Deleting time block series: id={}, userId={}", id, userId);
        if (seriesRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId).isEmpty()) {
            return Result.err(ErrorCode.NOT_FOUND, "Time block series not found");
        }
        Instant now = Instant.now(clock);
        seriesRepository.softDeleteByIdAndUserId(id, userId, now);
        timeBlockRepository.softDeleteFutureBySeriesId(id, userId, now, now);
        log.info("Time block series soft deleted: id={}, userId={}", id, userId);
        return Result.ok(null);
    }

    private LocalDate horizon(LocalDate endDate) {
        LocalDate max = LocalDate.now(clock).plusMonths(12);
        if (endDate != null && endDate.isBefore(max)) {
            return endDate;
        }
        return max;
    }

    private List<TimeBlock> buildOccurrences(UUID userId, TimeBlockSeries series, RecurrenceRule rule,
            LocalDate from, LocalDate to) {
        List<TimeBlock> occurrences = new ArrayList<>();
        Instant now = Instant.now(clock);
        for (LocalDate date : rule.datesBetween(from, to)) {
            Instant start = rule.startInstant(date);
            TimeBlock block = new TimeBlock();
            block.setUserId(userId);
            block.setTitle(series.getTitle());
            block.setStartDateTime(start);
            block.setEndDateTime(start.plusSeconds((long) rule.getDurationMinutes() * 60));
            block.setProjectId(series.getProjectId());
            block.setSeriesId(series.getId());
            block.setOverride(false);
            block.setCreatedAt(now);
            occurrences.add(block);
        }
        return occurrences;
    }

    private Result<Void> validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            return Result.err(ErrorCode.INVALID_SERIES_PATTERN, "Series title must be between 1 and 200 characters");
        }
        return Result.ok(null);
    }

    private Result<Void> validateProject(UUID userId, UUID projectId) {
        boolean owned = projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId).isPresent();
        if (!owned) {
            return Result.err(ErrorCode.NOT_FOUND, "Project not found");
        }
        return Result.ok(null);
    }
}
