package com.felipemelozx.kairos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateTimeBlockRequest(
    @NotBlank @Size(max = 200) String title,
    @NotNull Instant startDateTime,
    @NotNull Instant endDateTime,
    UUID projectId,
    UUID seriesId
) {}
