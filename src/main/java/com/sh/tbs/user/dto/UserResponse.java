package com.sh.tbs.user.dto;

import com.sh.tbs.user.UserDetail;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UUID locationId,
        String locationName,
        Instant createdAt
) {
    public static UserResponse from(UserDetail u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getLocation() != null ? u.getLocation().getId() : null,
                u.getLocation() != null ? u.getLocation().getName() : null,
                u.getCreatedAt()
        );
    }
}
