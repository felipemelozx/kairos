package com.felipemelozx.kairos.dto.response;

import com.felipemelozx.kairos.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String name,
    String avatarUrl,
    String provider,
    boolean active,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getAvatarUrl(),
            user.getProvider().name(),
            user.getActive(),
            user.getCreatedAt()
        );
    }
}
