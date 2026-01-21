package com.alura.churnnsight.controller;

import com.alura.churnnsight.dto.BatchProResponse;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.dto.consult.DataPredictionDetail;
import com.alura.churnnsight.dto.integration.DataIntegrationRequest;
import com.alura.churnnsight.dto.integration.DataIntegrationResponse;
import com.alura.churnnsight.service.PredictionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/predict")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @PostMapping("/{customerId}")
    public Mono<ResponseEntity<DataPredictionResult>> inferPrediction(
            @PathVariable @NotBlank(message="El customerId no puede estar vacío") String customerId) {
        return predictionService.predictForCustomer(customerId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Page<DataPredictionDetail>> getPredictions(@PathVariable String customerId, @PageableDefault(size = 20) Pageable pageable){
        var page = predictionService.getPredictionsByCustomerId(customerId, pageable);
        return ResponseEntity.ok(page);
    }
    @GetMapping("/{customerId}/latest")
    public ResponseEntity<DataPredictionDetail> getPredictions(@PathVariable String customerId){
        Pageable pageable = PageRequest.of(
                0,
                1,
                Sort.by(Sort.Direction.DESC, "predictedAt"));

        Page<DataPredictionDetail> page = predictionService.getPredictionsByCustomerId(customerId, pageable);
        return page.hasContent()
                ? ResponseEntity.ok(page.getContent().get(0))
                : ResponseEntity.notFound().build();
    }

    // PARA MVP
    @PostMapping("/integration")
    public Mono<ResponseEntity<DataIntegrationResponse>> inferPredictionIntegration(
            @Valid @RequestBody DataIntegrationRequest request
    ) {
        return predictionService.predictIntegration(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/integration/{customerId}")
    public Mono<ResponseEntity<DataIntegrationResponse>> inferIntegrationFromDb(
            @PathVariable String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate refDate
    ) {
        return predictionService.predictIntegrationFromDbPro(customerId, refDate)
                .map(ResponseEntity::ok);
    }

    //Ejecuta batch desde JSON
    @PostMapping("/integration/batch/pro")
    public Mono<ResponseEntity<BatchProResponse>> inferPredictionIntegrationBatchPro(
            @Valid @RequestBody List<@Valid DataIntegrationRequest> requestList
    ) {
        return predictionService.predictIntegrationBatchPro(requestList)
                .map(ResponseEntity::ok);
    }

    //Ejecuta batch de BD
    @PostMapping("/integration/batch/pro/all")
    public Mono<ResponseEntity<BatchProResponse>> runBatchProAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate refDate
    ) {
        return predictionService.predictIntegrationBatchProAll(refDate)
                .map(ResponseEntity::ok);
    }


}
