package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.BatchRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface BatchRunRepository extends JpaRepository<BatchRun, Long> {
    Optional<BatchRun> findByBucketDateAndBatchHash(LocalDate bucketDate, String batchHash);
}
