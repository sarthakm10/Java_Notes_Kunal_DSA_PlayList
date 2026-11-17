package com.notes.tasks.repo;

import com.notes.tasks.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);    // derived query: Spring writes the JPQL from the name
}
