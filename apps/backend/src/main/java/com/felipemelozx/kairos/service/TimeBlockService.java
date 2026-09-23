package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.common.ErrorCode;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.CreateTimeBlockRequest;
import com.felipemelozx.kairos.dto.request.UpdateTimeBlockRequest;
import com.felipemelozx.kairos.dto.response.TimeBlockResponse;
import com.felipemelozx.kairos.entity.TimeBlock;
import com.felipemelozx.kairos.repository.ProjectRepository;
import com.felipemelozx.kairos.repository.TimeBlockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TimeBlockService {

    private static final Logger log = LoggerFactory.getLogger(TimeBlockService.class);
    private static final int MAX_TITLE_LENGTH = 200;

    private final TimeBlockRepository timeBlockRepository;
    private final ProjectRepository projectRepository;

    public TimeBlockService(TimeBlockRepository timeBlockRepository, ProjectRepository projectRepository) {
        this.timeBlockRepository = timeBlockRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Result<TimeBlockResponse> create(UUID userId, CreateTimeBlockRequest request) {
        log.info("Creating time block for userId={}", userId);
        Result<Void> titleCheck = validateTitle(request.title());
        if (titleCheck instanceof Result.Err<Void> err) {
            return Result.err(err.error());
        }
        Result<Void> rangeCheck = validateRange(request.startDateTime(), request.endDateTime());
        if (rangeCheck instanceof Result.Err<Void> err) {
            return Result.err(err.error());
        }
        if (request.projectId() != null) {
            Result<Void> projectCheck = validateProject(userId, request.projectId());
            if (projectCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
        }

        TimeBlock block = new TimeBlock();
        block.setUserId(userId);
        block.setTitle(request.title());
        block.setStartDateTime(request.startDateTime());
        block.setEndDateTime(request.endDateTime());
        block.setProjectId(request.projectId());
        block.setSeriesId(request.seriesId());
        block.setOverride(false);
        block.setCreatedAt(Instant.now());

        TimeBlock saved = timeBlockRepository.save(block);
        log.info("Time block created: id={}, userId={}", saved.getId(), userId);
        return Result.ok(TimeBlockResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public Result<List<TimeBlockResponse>> listByUser(UUID userId, Instant from, Instant to) {
        log.info("Listing time blocks for userId={}", userId);
        if ((from == null) != (to == null)) {
            return Result.err(ErrorCode.INVALID_TIME_RANGE, "Both from and to must be provided together");
        }
        if (from != null) {
            Result<Void> rangeCheck = validateRange(from, to);
            if (rangeCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
        }
        List<TimeBlock> blocks;
        if (from != null && to != null) {
            blocks = timeBlockRepository
                    .findByUserIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThanAndDeletedAtIsNullOrderByStartDateTimeAsc(
                            userId, from, to);
        } else {
            blocks = timeBlockRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(userId);
        }
        List<TimeBlockResponse> result = blocks.stream().map(TimeBlockResponse::from).toList();
        log.info("Listed {} time blocks for userId={}", result.size(), userId);
        return Result.ok(result);
    }

    @Transactional(readOnly = true)
    public Result<TimeBlockResponse> getById(UUID id, UUID userId) {
        log.info("Fetching time block: id={}, userId={}", id, userId);
        return timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .map(block -> Result.<TimeBlockResponse>ok(TimeBlockResponse.from(block)))
                .orElseGet(() -> Result.err(ErrorCode.NOT_FOUND, "Time block not found"));
    }

    @Transactional
    public Result<TimeBlockResponse> update(UUID id, UUID userId, UpdateTimeBlockRequest request) {
        log.info("Updating time block: id={}, userId={}", id, userId);
        var maybeBlock = timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
        if (maybeBlock.isEmpty()) {
            return Result.err(ErrorCode.NOT_FOUND, "Time block not found");
        }
        TimeBlock block = maybeBlock.get();

        if (request.title() != null) {
            Result<Void> titleCheck = validateTitle(request.title());
            if (titleCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
        }

        Instant newStart = request.startDateTime() != null ? request.startDateTime() : block.getStartDateTime();
        Instant newEnd = request.endDateTime() != null ? request.endDateTime() : block.getEndDateTime();
        if (request.startDateTime() != null || request.endDateTime() != null) {
            Result<Void> rangeCheck = validateRange(newStart, newEnd);
            if (rangeCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
        }

        if (request.projectId() != null) {
            Result<Void> projectCheck = validateProject(userId, request.projectId());
            if (projectCheck instanceof Result.Err<Void> err) {
                return Result.err(err.error());
            }
        }

        if (request.title() != null) {
            block.setTitle(request.title());
        }
        if (request.startDateTime() != null) {
            block.setStartDateTime(request.startDateTime());
        }
        if (request.endDateTime() != null) {
            block.setEndDateTime(request.endDateTime());
        }
        if (request.projectId() != null) {
            block.setProjectId(request.projectId());
        }

        TimeBlock saved = timeBlockRepository.save(block);
        log.info("Time block updated: id={}, userId={}", saved.getId(), userId);
        return Result.ok(TimeBlockResponse.from(saved));
    }

    @Transactional
    public Result<Void> softDelete(UUID id, UUID userId) {
        log.info("Deleting time block: id={}, userId={}", id, userId);
        if (timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId).isEmpty()) {
            return Result.err(ErrorCode.NOT_FOUND, "Time block not found");
        }

        timeBlockRepository.softDeleteByIdAndUserId(id, userId, Instant.now());
        log.info("Time block soft deleted: id={}, userId={}", id, userId);
        return Result.ok(null);
    }

    private Result<Void> validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
            return Result.err(ErrorCode.INVALID_TIME_BLOCK_TITLE);
        }
        return Result.ok(null);
    }

    private Result<Void> validateRange(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return Result.err(ErrorCode.INVALID_TIME_RANGE);
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
