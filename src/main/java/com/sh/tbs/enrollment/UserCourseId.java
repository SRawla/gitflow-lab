package com.sh.tbs.enrollment;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCourseId implements Serializable {
    private UUID userId;
    private UUID courseId;
}
