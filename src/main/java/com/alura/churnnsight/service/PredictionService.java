package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.model.Customer;
import com.alura.churnnsight.model.Prediction;
import com.alura.churnnsight.model.enumeration.Prevision;
import com.alura.churnnsight.repository.CustomerRepository;
import com.alura.churnnsight.repository.PredictionRepository;
import com.alura.churnnsight.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
public class PredictionService {

    private final FastApiClient fastApiClient;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    public PredictionService(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }


    public Mono<DataPredictionResult> predictForClient(String clientId) {

        return Mono.fromCallable(() -> {

            Customer customer = customerRepository
                    .findByCustomerIdIgnoreCase(clientId)
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

            Long id = customer.getId();

            int isActiveMember =
                    customer.getStatus() != null &&
                            Boolean.TRUE.equals(customer.getStatus().getIsActiveMember())
                            ? 1
                            : 0;

            DataMakePrediction data = new DataMakePrediction(
                    customer,
                    customerRepository.CountBalanceByCostumerId(id),
                    customerRepository.CountProductsByCostumerId(id),
                    isActiveMember
            );

            DataPredictionResult response = fastApiClient
                    .predict(data)
                    .block();

            if(response == null){
                throw new IllegalStateException("Prediction services returned null");
            }
            predictionRepository.save(new Prediction(response, customer));

            return response;

        }).subscribeOn(Schedulers.boundedElastic());
    }


}
