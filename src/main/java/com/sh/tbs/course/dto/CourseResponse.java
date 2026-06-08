package com.sh.tbs.course.dto;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(UUID id, String name, String description, Instant createdAt) {}
