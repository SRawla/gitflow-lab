package com.sh.tbs.enrollment;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EnrollRequest(@NotNull UUID userId) {}
