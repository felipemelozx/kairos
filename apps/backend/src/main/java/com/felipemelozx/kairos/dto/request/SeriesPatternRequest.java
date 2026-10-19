package com.felipemelozx.kairos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SeriesPatternRequest(
    @NotBlank String type,
    Integer interval,
    List<Integer> weekdays,
    @NotBlank String time,
    @NotNull Integer durationMinutes
) {}
