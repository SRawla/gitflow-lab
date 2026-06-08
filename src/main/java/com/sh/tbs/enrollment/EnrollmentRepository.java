package com.sh.tbs.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<UserCourseAssignment, UserCourseId> {
    List<UserCourseAssignment> findByCourseId(UUID courseId);
    List<UserCourseAssignment> findByUserId(UUID userId);
}
