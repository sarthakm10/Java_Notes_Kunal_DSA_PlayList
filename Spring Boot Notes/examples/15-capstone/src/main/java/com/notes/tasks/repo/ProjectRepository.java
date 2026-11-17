package com.notes.tasks.repo;

import com.notes.tasks.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/** JpaRepository<Entity, IdType> gives save, findById, findAll, deleteById, count... for free. */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
