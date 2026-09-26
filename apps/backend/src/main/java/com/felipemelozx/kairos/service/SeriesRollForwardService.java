package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.entity.TimeBlock;
import com.felipemelozx.kairos.entity.TimeBlockSeries;
import com.felipemelozx.kairos.repository.TimeBlockRepository;
import com.felipemelozx.kairos.repository.TimeBlockSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extends indefinite series (endDate = null) up to the 12-month horizon.
 * Idempotent: occurrences already materialized are skipped, and the unique
 * partial index (series_id, start_datetime) WHERE deleted_at IS NULL
 * guards against concurrent duplicates.
 */
@Service
public class SeriesRollForwardService {

    private static final Logger log = LoggerFactory.getLogger(SeriesRollForwardService.class);

    private final TimeBlockSeriesRepository seriesRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final Clock clock;

    @Autowired
    public SeriesRollForwardService(TimeBlockSeriesRepository seriesRepository,
            TimeBlockRepository timeBlockRepository) {
        this(seriesRepository, timeBlockRepository, Clock.systemUTC());
    }

    public SeriesRollForwardService(TimeBlockSeriesRepository seriesRepository,
            TimeBlockRepository timeBlockRepository,
            Clock clock) {
        this.seriesRepository = seriesRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void rollForward() {
        int created = extendIndefiniteSeries();
        log.info("Series roll-forward completed: {} occurrences created", created);
    }

    int extendIndefiniteSeries() {
        LocalDate horizon = LocalDate.now(clock).plusMonths(12);
        Instant now = Instant.now(clock);
        int created = 0;
        for (TimeBlockSeries series : seriesRepository.findByEndDateIsNullAndDeletedAtIsNull()) {
            RecurrenceRule rule;
            try {
                rule = RecurrenceRule.parseJson(series.getPattern());
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping series with invalid pattern: id={}", series.getId());
                continue;
            }
            Set<Instant> existing = timeBlockRepository
                    .findBySeriesIdAndUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(
                            series.getId(), series.getUserId())
                    .stream()
                    .map(TimeBlock::getStartDateTime)
                    .collect(Collectors.toSet());
            List<TimeBlock> missing = new ArrayList<>();
            for (LocalDate date : rule.datesBetween(series.getStartDate(), horizon)) {
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
                created += missing.size();
                log.info("Series roll-forward extended series id={} with {} occurrences", series.getId(),
                        missing.size());
            }
        }
        return created;
    }
}
