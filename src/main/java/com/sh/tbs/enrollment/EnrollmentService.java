package com.sh.tbs.enrollment;

import com.sh.tbs.common.ResourceNotFoundException;
import com.sh.tbs.course.CourseRepository;
import com.sh.tbs.user.UserDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository repository;
    private final CourseRepository courseRepository;
    private final UserDetailRepository userRepository;

    public List<UUID> findUsersByCourse(UUID courseId) {
        if (!courseRepository.existsById(courseId))
            throw new ResourceNotFoundException("Course not found: " + courseId);
        return repository.findByCourseId(courseId).stream()
                .map(UserCourseAssignment::getUserId).toList();
    }

    public List<UUID> findCoursesByUser(UUID userId) {
        if (!userRepository.existsById(userId))
            throw new ResourceNotFoundException("User not found: " + userId);
        return repository.findByUserId(userId).stream()
                .map(UserCourseAssignment::getCourseId).toList();
    }

    public void enroll(UUID courseId, UUID userId) {
        if (!courseRepository.existsById(courseId))
            throw new ResourceNotFoundException("Course not found: " + courseId);
        if (!userRepository.existsById(userId))
            throw new ResourceNotFoundException("User not found: " + userId);
        var assignment = UserCourseAssignment.builder()
                .courseId(courseId).userId(userId).build();
        repository.save(assignment);
    }

    public void unenroll(UUID courseId, UUID userId) {
        var id = new UserCourseId(userId, courseId);
        if (!repository.existsById(id))
            throw new ResourceNotFoundException("Enrollment not found");
        repository.deleteById(id);
    }
}
