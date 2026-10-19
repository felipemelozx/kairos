package com.felipemelozx.kairos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSeriesRequest(
    @NotBlank @Size(max = 200) String title,
    UUID projectId,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @NotNull @Valid SeriesPatternRequest pattern
) {}
