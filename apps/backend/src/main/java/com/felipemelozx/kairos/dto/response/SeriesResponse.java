package com.felipemelozx.kairos.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipemelozx.kairos.entity.TimeBlockSeries;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SeriesResponse(
    UUID id,
    UUID userId,
    String title,
    UUID projectId,
    LocalDate startDate,
    LocalDate endDate,
    JsonNode pattern,
    Instant createdAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static SeriesResponse from(TimeBlockSeries series) {
        JsonNode pattern;
        try {
            pattern = MAPPER.readTree(series.getPattern());
        } catch (Exception ex) {
            throw new IllegalStateException("Stored series pattern is not valid JSON", ex);
        }
        return new SeriesResponse(
            series.getId(),
            series.getUserId(),
            series.getTitle(),
            series.getProjectId(),
            series.getStartDate(),
            series.getEndDate(),
            pattern,
            series.getCreatedAt()
        );
    }
}
