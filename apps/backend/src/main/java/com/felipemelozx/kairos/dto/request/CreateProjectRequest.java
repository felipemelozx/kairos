package com.felipemelozx.kairos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color
) {}
