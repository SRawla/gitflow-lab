package com.sh.tbs.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDetailRepository extends JpaRepository<UserDetail, UUID> {

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserDetail u LEFT JOIN FETCH u.location")
    List<UserDetail> findAllWithLocation();

    @Query("SELECT u FROM UserDetail u LEFT JOIN FETCH u.location WHERE u.id = :id")
    Optional<UserDetail> findByIdWithLocation(UUID id);
}
