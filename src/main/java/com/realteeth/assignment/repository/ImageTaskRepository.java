package com.realteeth.assignment.repository;

import com.realteeth.assignment.domain.ImageTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageTaskRepository extends JpaRepository<ImageTask, Long> {

    Optional<ImageTask> findByIdempotencyKey(String idempotencyKey);
}
