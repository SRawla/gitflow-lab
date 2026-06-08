package com.sh.tbs.enrollment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_course")
@IdClass(UserCourseId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCourseAssignment {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "course_id")
    private UUID courseId;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false, nullable = false)
    private Instant assignedAt;
}
