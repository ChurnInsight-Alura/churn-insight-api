package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.dto.consult.DataPredictionDetail;
import com.alura.churnnsight.dto.integration.*;
import com.alura.churnnsight.model.*;
import com.alura.churnnsight.model.enumeration.InterventionPriority;
import com.alura.churnnsight.model.enumeration.Prevision;
import com.alura.churnnsight.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PredictionService {

    private final FastApiClient fastApiClient;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;

    @Autowired
    private CustomerSessionRepository customerSessionRepository;

    public PredictionService(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }


    public Mono<DataPredictionResult> predictForCustomer(String customerId) {

        return Mono.fromCallable(() ->
                predictAndPersist(customerId)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    protected DataPredictionResult predictAndPersist(String customerId) {

        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Long id = customer.getId();

        int isActiveMember = customer.getStatus() != null && Boolean.TRUE.equals(customer.getStatus().getIsActiveMember()) ? 1 : 0;
        DataMakePrediction data = new DataMakePrediction(
                customer,
                customerRepository.CountBalanceByCostumerId(id),
                customerRepository.CountProductsByCostumerId(id),
                isActiveMember
        );

        DataPredictionResult response = fastApiClient.predict(data)
                .block();

        if (response == null) {
            throw new IllegalStateException("Prediction services returned null");
        }

        predictionRepository.save(new Prediction(response, customer));

        return response;
    }

    public Page<DataPredictionDetail> getPredictionsByCustomerId(String customerId, Pageable pageable) {
        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        return predictionRepository.findByCustomerId(customer.getId(),pageable).map(DataPredictionDetail::new);
    }

    public Mono<DataIntegrationResponse> predictIntegration(DataIntegrationRequest request) {
        return fastApiClient.predictIntegration(request);
    }

    public Mono<DataIntegrationResponse> predictIntegrationFromDb(String customerId, LocalDate refDate) {
        LocalDate effectiveRefDate = (refDate != null) ? refDate : LocalDate.now();
        return Mono.fromCallable(() -> predictIntegrationFromDbAndPersist(customerId, effectiveRefDate))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    protected DataIntegrationResponse predictIntegrationFromDbAndPersist(String customerId, LocalDate refDate) {

        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        // RowNumber: usar el ID autoincrement como "fila"
        Integer rowNumber = customer.getId() == null ? null : customer.getId().intValue();

        // Tenure: meses calculados desde created_at a refDate (por defecto hoy)
        int tenureMonths = customer.getTenure(refDate);

        // Balance (usa tu query actual si suma balance)
        Float balance = customerRepository.CountBalanceByCostumerId(customer.getId());
        float balanceF = (balance == null) ? 0f : balance.floatValue();

        // Products
        Integer numProductsDb = customerRepository.CountProductsByCostumerId(customer.getId());
        int numProducts = (numProductsDb == null) ? 0 : numProductsDb;


        // Status fields
        CustomerStatus st = customerRepository.findStatusByCustomerId(customer.getId());

        Integer creditScore = (st != null) ? st.getCreditScore() : null;
        int isActiveMember = (st != null && Boolean.TRUE.equals(st.getIsActiveMember())) ? 1 : 0;
        int hasCrCard = (st != null && Boolean.TRUE.equals(st.getHasCrCard())) ? 1 : 0;

        // Gender string (define con Data si quiere "MALE" o "Male")
        String gender = (customer.getGender() == null) ? null : customer.getGender().name();

        // Transacciones y sesiones desde BD
        var txs = customerTransactionRepository.findByCustomerId(customer.getId());
        var ses = customerSessionRepository.findByCustomerId(customer.getId());

        DataIntegrationRequest req = new DataIntegrationRequest(
                new ClienteIn(
                        rowNumber,
                        customer.getCustomerId(),
                        customer.getSurname(),
                        creditScore,
                        customer.getGeography(),
                        gender,
                        customer.getAge(),
                        tenureMonths,
                        balanceF,
                        numProducts,
                        hasCrCard,
                        isActiveMember,
                        customer.getEstimatedSalary() == null ? null : customer.getEstimatedSalary().floatValue(),
                        customer.getCustomerSegment()
                ),
                txs.stream().map(t -> new TransaccionIn(
                        t.getTransactionId(),
                        customer.getCustomerId(),
                        t.getTransactionDate(),
                        (float) t.getAmount(),
                        t.getTransactionType()
                )).toList(),
                ses.stream().map(s -> new SesionIn(
                        s.getSessionId(),
                        customer.getCustomerId(),
                        s.getSessionDate(),
                        (float) s.getDurationMin(),
                        s.getUsedTransfer(),
                        s.getUsedPayment(),
                        s.getUsedInvest(),
                        s.getOpenedPush(),
                        s.getFailedLogin()
                )).toList()
        );

        // Llamar a Data
        DataIntegrationResponse response = fastApiClient.predictIntegration(req).block();
        if (response == null) throw new IllegalStateException("Prediction services returned null");

        // Fecha lógica de predicción QUINCENAL (bucket 1 o 16)
        LocalDate execDate = (refDate != null) ? refDate : LocalDate.now();
        LocalDate bucketDate = getQuincenaBucket(execDate);

        // Buscar si ya existe predicción para esta quincena
        Prediction prediction = predictionRepository
                .findByCustomerIdAndPredictionDate(customer.getId(), bucketDate)
                .orElseGet(Prediction::new);

        // Setear campos (INSERT o UPDATE)
        prediction.setCustomer(customer);
        prediction.setPredictionDate(bucketDate);
        prediction.setPredictedProba(response.predictedProba());
        prediction.setPredictedLabel(response.predictedLabel());
        prediction.setCustomerSegment(response.customerSegment());
        prediction.setInterventionPriority(
                InterventionPriority.fromString(response.interventionPriority())
        );

        // Guardar (insert o update)
        predictionRepository.save(prediction);

        return response;
    }

    private LocalDate getQuincenaBucket(LocalDate date) {
        return (date.getDayOfMonth() <= 15)
                ? date.withDayOfMonth(1)
                : date.withDayOfMonth(16);
    }

}
