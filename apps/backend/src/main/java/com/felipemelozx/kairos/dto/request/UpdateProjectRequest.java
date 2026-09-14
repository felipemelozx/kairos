package com.felipemelozx.kairos.dto.request;

import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @Size(max = 100) String name,
    @Size(max = 500) String description,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
    ProjectStatus status
) {}
