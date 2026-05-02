package com.example.securityanalyze.user.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAll();

    List<User> findByStatus(UserStatus status);

    void updateStatus(Long id, UserStatus status);
}
