package com.alura.churnnsight.controller;

import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.dto.consult.DataPredictionDetail;
import com.alura.churnnsight.dto.integration.DataIntegrationRequest;
import com.alura.churnnsight.dto.integration.DataIntegrationResponse;
import com.alura.churnnsight.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import reactor.core.scheduler.Schedulers;


@RestController
@RequestMapping("/predict")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @PostMapping("/{customerId}")
    public Mono<ResponseEntity<DataPredictionResult>> inferPrediction(@PathVariable String customerId) {
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

    @PostMapping("/integration")
    public Mono<ResponseEntity<DataIntegrationResponse>> inferPredictionIntegration(
            @RequestBody DataIntegrationRequest request
    ) {
        return predictionService.predictIntegration(request)
                .map(ResponseEntity::ok);
    }
    @PostMapping("/integration/{customerId}")
    public Mono<ResponseEntity<DataPredictionDetail>> inferIntegrationFromDb(
            @PathVariable String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate refDate) {

        Mono<ResponseEntity<DataPredictionDetail>> latestMono =
                Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(
                            0,
                            1,
                            Sort.by(Sort.Direction.DESC, "predictedAt")
                    );

                    Page<DataPredictionDetail> page = predictionService.getPredictionsByCustomerId(customerId, pageable);

                    if (page.hasContent()) {
                        return ResponseEntity.ok(page.getContent().get(0));
                    }


                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .<DataPredictionDetail>body(null);
                }).subscribeOn(Schedulers.boundedElastic());

        return predictionService.predictIntegrationFromDb(customerId, refDate)
                .flatMap(ignored -> latestMono);
    }

    @PostMapping("/integration/batch")
    public Mono<ResponseEntity<List<DataIntegrationResponse>>> inferPredictionIntegrationBatch(
            @RequestBody List<DataIntegrationRequest> requestList
    ) {
        return predictionService.predictIntegrationBatchUpsertAndPersist(requestList)
                .map(ResponseEntity::ok);
    }


}
