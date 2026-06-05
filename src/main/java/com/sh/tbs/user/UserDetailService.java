package com.sh.tbs.user;

import com.sh.tbs.common.ResourceNotFoundException;
import com.sh.tbs.location.Location;
import com.sh.tbs.location.LocationRepository;
import com.sh.tbs.user.dto.UserRequest;
import com.sh.tbs.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailService {

    private final UserDetailRepository userRepository;
    private final LocationRepository locationRepository;

    public List<UserResponse> findAll() {
        return userRepository.findAllWithLocation().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse findById(UUID id) {
        return userRepository.findByIdWithLocation(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with email '" + request.email() + "' already exists");
        }

        Location location = resolveLocation(request.locationId());

        UserDetail user = UserDetail.builder()
                .name(request.name())
                .email(request.email())
                .location(location)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        UserDetail user = userRepository.findByIdWithLocation(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with email '" + request.email() + "' already exists");
        }

        Location location = resolveLocation(request.locationId());

        user.setName(request.name());
        user.setEmail(request.email());
        user.setLocation(location);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private Location resolveLocation(UUID locationId) {
        if (locationId == null) return null;
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
    }
}
