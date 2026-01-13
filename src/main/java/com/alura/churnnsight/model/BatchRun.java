package com.alura.churnnsight.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "batch_run",
        uniqueConstraints = @UniqueConstraint(name = "uq_batch_run_bucket_hash", columnNames = {"bucket_date", "batch_hash"}))
public class BatchRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="bucket_date", nullable=false)
    private LocalDate bucketDate;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="batch_hash", nullable=false, length=64)
    private String batchHash;

    @Lob
    @Column(name="stats_json")
    private String statsJson; // guardamos como string JSON (más portable)

}