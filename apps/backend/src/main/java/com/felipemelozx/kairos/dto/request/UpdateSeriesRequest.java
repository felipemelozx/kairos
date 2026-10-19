package com.felipemelozx.kairos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSeriesRequest(
    @NotNull LocalDate effectiveFrom,
    @Size(max = 200) String title,
    UUID projectId,
    LocalDate endDate,
    @Valid SeriesPatternRequest pattern
) {}
