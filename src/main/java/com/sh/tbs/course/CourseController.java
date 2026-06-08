package com.sh.tbs.course;

import com.sh.tbs.course.dto.CourseRequest;
import com.sh.tbs.course.dto.CourseResponse;
import com.sh.tbs.enrollment.EnrollRequest;
import com.sh.tbs.enrollment.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService service;
    private final EnrollmentService enrollmentService;

    @GetMapping
    public List<CourseResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    // --- Enrollment endpoints (Feature 2) ---

    @GetMapping("/{id}/enrollees")
    public List<UUID> getEnrollees(@PathVariable UUID id) {
        return enrollmentService.findUsersByCourse(id);
    }

    @PostMapping("/{id}/enrollees")
    @ResponseStatus(HttpStatus.CREATED)
    public void enroll(@PathVariable UUID id, @Valid @RequestBody EnrollRequest request) {
        enrollmentService.enroll(id, request.userId());
    }

    @DeleteMapping("/{id}/enrollees/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable UUID id, @PathVariable UUID userId) {
        enrollmentService.unenroll(id, userId);
    }
}
