package com.alura.churnnsight.dto;

import com.alura.churnnsight.dto.integration.DataIntegrationResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record BatchProResponse(
        LocalDate bucketDate,
        Long batchRunId,
        String batchHash,
        Map<String, Object> stats,
        List<DataIntegrationResponse> predictions
) {}
