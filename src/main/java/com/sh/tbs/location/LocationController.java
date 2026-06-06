package com.sh.tbs.location;

import com.sh.tbs.location.dto.LocationRequest;
import com.sh.tbs.location.dto.LocationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService service;

    @GetMapping
    public List<LocationResponse> findAll() {
        log.info("GET /locations");
        return service.findAll();
    }

    @GetMapping("/{id}")
    public LocationResponse findById(@PathVariable UUID id) {
        log.info("GET /locations/{}", id);
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@Valid @RequestBody LocationRequest request) {
        log.info("POST /locations - {}", request.name());
        return service.create(request);
    }

    @PutMapping("/{id}")
    public LocationResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody LocationRequest request) {
        log.info("PUT /locations/{}", id);
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        log.info("DELETE /locations/{}", id);
        service.delete(id);
    }
}
