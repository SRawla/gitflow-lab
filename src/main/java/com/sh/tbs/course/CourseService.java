package com.sh.tbs.course;

import com.sh.tbs.common.ResourceNotFoundException;
import com.sh.tbs.course.dto.CourseRequest;
import com.sh.tbs.course.dto.CourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository repository;

    public List<CourseResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public CourseResponse findById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    public CourseResponse create(CourseRequest request) {
        Course course = Course.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return toResponse(repository.save(course));
    }

    public CourseResponse update(UUID id, CourseRequest request) {
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
        course.setName(request.name());
        course.setDescription(request.description());
        return toResponse(repository.save(course));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found: " + id);
        }
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }

    private CourseResponse toResponse(Course c) {
        return new CourseResponse(c.getId(), c.getName(), c.getDescription(), c.getCreatedAt());
    }
}
