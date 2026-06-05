package com.sh.tbs.location;

import com.sh.tbs.common.ResourceNotFoundException;
import com.sh.tbs.location.dto.LocationRequest;
import com.sh.tbs.location.dto.LocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository repository;

    public List<LocationResponse> findAll() {
        return repository.findAll().stream()
                .map(LocationResponse::from)
                .toList();
    }

    public LocationResponse findById(UUID id) {
        return repository.findById(id)
                .map(LocationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
    }

    @Transactional
    public LocationResponse create(LocationRequest request) {
        if (repository.existsByName(request.name())) {
            throw new IllegalArgumentException("Location with name '" + request.name() + "' already exists");
        }
        Location location = Location.builder()
                .name(request.name())
                .city(request.city())
                .country(request.country())
                .build();
        return LocationResponse.from(repository.save(location));
    }

    @Transactional
    public LocationResponse update(UUID id, LocationRequest request) {
        Location location = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));

        if (!location.getName().equals(request.name()) && repository.existsByName(request.name())) {
            throw new IllegalArgumentException("Location with name '" + request.name() + "' already exists");
        }

        location.setName(request.name());
        location.setCity(request.city());
        location.setCountry(request.country());
        return LocationResponse.from(repository.save(location));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Location not found: " + id);
        }
        repository.deleteById(id);
    }
}
