package com.sh.tbs.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @Size(max = 120, message = "City must not exceed 120 characters")
        String city,

        @Size(max = 80, message = "Country must not exceed 80 characters")
        String country
) {}
