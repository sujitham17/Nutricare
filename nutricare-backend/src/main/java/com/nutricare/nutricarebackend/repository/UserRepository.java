package com.nutricare.nutricarebackend.repository;

import com.nutricare.nutricarebackend.entity.Role;
import com.nutricare.nutricarebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    List<User> findByRoleInOrderByCreatedAtDesc(List<Role> roles);

    List<User> findByRole(Role role);
}
