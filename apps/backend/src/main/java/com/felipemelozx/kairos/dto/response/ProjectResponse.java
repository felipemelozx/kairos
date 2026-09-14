package com.felipemelozx.kairos.dto.response;

import com.felipemelozx.kairos.entity.Project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    UUID userId,
    String name,
    String description,
    String color,
    String status,
    Instant createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getUserId(),
            project.getName(),
            project.getDescription(),
            project.getColor(),
            project.getStatus().name(),
            project.getCreatedAt()
        );
    }
}
