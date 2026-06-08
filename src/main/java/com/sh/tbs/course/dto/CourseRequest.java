package com.sh.tbs.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String description
) {}
