package com.felipemelozx.kairos.dto.request;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateTimeBlockRequest(
    @Size(max = 200) String title,
    Instant startDateTime,
    Instant endDateTime,
    UUID projectId
) {}
