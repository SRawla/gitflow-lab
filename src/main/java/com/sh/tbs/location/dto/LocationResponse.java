package com.sh.tbs.location.dto;

import com.sh.tbs.location.Location;

import java.time.Instant;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        String city,
        String country,
        Instant createdAt
) {
    public static LocationResponse from(Location l) {
        return new LocationResponse(l.getId(), l.getName(), l.getCity(), l.getCountry(), l.getCreatedAt());
    }
}
