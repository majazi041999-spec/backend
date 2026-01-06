package com.taskchi.taskchi.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByManagerId(Long id);

    @Query("select u from User u left join fetch u.manager")
    List<User> findAllWithManager();
}