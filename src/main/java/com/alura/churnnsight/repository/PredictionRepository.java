package com.alura.churnnsight.repository;

import com.alura.churnnsight.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionRepository extends JpaRepository<Prediction,Long> {
}
