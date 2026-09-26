package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.entity.TimeBlock;
import com.felipemelozx.kairos.entity.TimeBlockSeries;
import com.felipemelozx.kairos.repository.TimeBlockRepository;
import com.felipemelozx.kairos.repository.TimeBlockSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extends active series (indefinite or finite with endDate not yet reached)
 * up to the 12-month horizon.
 * Incremental: expansion starts after the last materialized occurrence,
 * so old/long series always reach the current horizon.
 * Each series runs in its own transaction so one bad series never
 * rolls back the whole batch.
 */
@Service
public class SeriesRollForwardService {

    private static final Logger log = LoggerFactory.getLogger(SeriesRollForwardService.class);

    private final TimeBlockSeriesRepository seriesRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final Clock clock;
    private final ObjectProvider<SeriesRollForwardService> self;

    @Autowired
    public SeriesRollForwardService(TimeBlockSeriesRepository seriesRepository,
            TimeBlockRepository timeBlockRepository,
            ObjectProvider<SeriesRollForwardService> self) {
        this(seriesRepository, timeBlockRepository, Clock.systemUTC(), self);
    }

    public SeriesRollForwardService(TimeBlockSeriesRepository seriesRepository,
            TimeBlockRepository timeBlockRepository,
            Clock clock,
            ObjectProvider<SeriesRollForwardService> self) {
        this.seriesRepository = seriesRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.clock = clock;
        this.self = self;
    }

    /** Test-friendly constructor without Spring proxy (single-transaction fallback). */
    public SeriesRollForwardService(TimeBlockSeriesRepository seriesRepository,
            TimeBlockRepository timeBlockRepository,
            Clock clock) {
        this(seriesRepository, timeBlockRepository, clock, null);
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void rollForward() {
        int created = extendIndefiniteSeries();
        log.info("Series roll-forward completed: {} occurrences created", created);
    }

    int extendIndefiniteSeries() {
        LocalDate today = LocalDate.now(clock);
        LocalDate horizon12m = today.plusMonths(12);
        Instant now = Instant.now(clock);
        int created = 0;
        List<TimeBlockSeries> active = seriesRepository.findActiveForRollForward(today);
        for (TimeBlockSeries series : active) {
            try {
                if (self != null) {
                    created += self.getObject().extendOneSeries(series.getId(), horizon12m, now);
                } else {
                    created += extendOneSeries(series.getId(), horizon12m, now);
                }
            } catch (DataIntegrityViolationException ex) {
                log.warn("Roll-forward conflict for series id={}, continuing", series.getId());
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping series with invalid pattern: id={}", series.getId());
            }
        }
        return created;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int extendOneSeries(java.util.UUID seriesId, LocalDate horizon12m, Instant now) {
        var maybe = seriesRepository.findById(seriesId);
        if (maybe.isEmpty() || maybe.get().getDeletedAt() != null) {
            return 0;
        }
        TimeBlockSeries series = maybe.get();
        RecurrenceRule rule;
        try {
            rule = RecurrenceRule.parseJson(series.getPattern());
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping series with invalid pattern: id={}", series.getId());
            return 0;
        }
        if (rule.getType() == RecurrenceRule.Type.SINGLE) {
            return 0;
        }
        LocalDate horizon = series.getEndDate() != null && series.getEndDate().isBefore(horizon12m)
                ? series.getEndDate()
                : horizon12m;

        Set<Instant> existing = timeBlockRepository
                .findBySeriesIdAndUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(
                        series.getId(), series.getUserId())
                .stream()
                .map(TimeBlock::getStartDateTime)
                .collect(Collectors.toSet());

        LocalDate windowStart = series.getStartDate();
        if (!existing.isEmpty()) {
            LocalDate last = existing.stream()
                    .map(i -> i.atZone(ZoneOffset.UTC).toLocalDate())
                    .max(LocalDate::compareTo)
                    .orElse(series.getStartDate());
            LocalDate afterLast = last.plusDays(1);
            if (afterLast.isAfter(windowStart)) {
                windowStart = afterLast;
            }
        }
        if (windowStart.isAfter(horizon)) {
            return 0;
        }
        List<TimeBlock> missing = new ArrayList<>();
        for (LocalDate date : rule.datesBetween(windowStart, horizon)) {
            Instant start = rule.startInstant(date);
            if (existing.contains(start)) {
                continue;
            }
            TimeBlock block = new TimeBlock();
            block.setUserId(series.getUserId());
            block.setTitle(series.getTitle());
            block.setStartDateTime(start);
            block.setEndDateTime(start.plusSeconds((long) rule.getDurationMinutes() * 60));
            block.setProjectId(series.getProjectId());
            block.setSeriesId(series.getId());
            block.setOverride(false);
            block.setCreatedAt(now);
            missing.add(block);
        }
        if (!missing.isEmpty()) {
            timeBlockRepository.saveAll(missing);
            log.info("Series roll-forward extended series id={} with {} occurrences", series.getId(),
                    missing.size());
            return missing.size();
        }
        return 0;
    }
}
