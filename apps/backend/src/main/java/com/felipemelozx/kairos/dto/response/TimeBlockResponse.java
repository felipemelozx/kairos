package com.felipemelozx.kairos.dto.response;

import com.felipemelozx.kairos.entity.TimeBlock;

import java.time.Instant;
import java.util.UUID;

public record TimeBlockResponse(
    UUID id,
    UUID userId,
    String title,
    Instant startDateTime,
    Instant endDateTime,
    UUID projectId,
    UUID seriesId,
    boolean isOverride,
    Instant createdAt
) {
    public static TimeBlockResponse from(TimeBlock block) {
        return new TimeBlockResponse(
            block.getId(),
            block.getUserId(),
            block.getTitle(),
            block.getStartDateTime(),
            block.getEndDateTime(),
            block.getProjectId(),
            block.getSeriesId(),
            block.isOverride(),
            block.getCreatedAt()
        );
    }
}
