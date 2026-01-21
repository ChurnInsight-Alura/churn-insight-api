package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.client.LlmClient;
import com.alura.churnnsight.dto.integration.DataIntegrationRequest;
import com.alura.churnnsight.dto.integration.DataIntegrationResponse;
import com.alura.churnnsight.model.*;
import com.alura.churnnsight.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PredictionServiceHackathonFlowTest {

    private PredictionService predictionService;
    private FastApiClient fastApiClient;
    private ObjectMapper mapper;
    private CustomerRepository customerRepository;
    private CustomerStatusRepository customerStatusRepository;
    private AccountRepository accountRepository;
    private PredictionRepository predictionRepository;
    private CustomerTransactionRepository customerTransactionRepository;
    private CustomerSessionRepository customerSessionRepository;
    private LlmClient llmClient;
    private BatchRunRepository batchRunRepository;
    private BatchRunCustomerRepository batchRunCustomerRepository;
    private PlatformTransactionManager txManager;

    @BeforeEach
    void setUp() throws Exception {
        fastApiClient = mock(FastApiClient.class);
        mapper = new ObjectMapper();

        customerRepository = mock(CustomerRepository.class);
        customerStatusRepository = mock(CustomerStatusRepository.class);
        accountRepository = mock(AccountRepository.class);
        predictionRepository = mock(PredictionRepository.class);
        customerTransactionRepository = mock(CustomerTransactionRepository.class);
        customerSessionRepository = mock(CustomerSessionRepository.class);
        llmClient = mock(LlmClient.class);
        batchRunRepository = mock(BatchRunRepository.class);
        batchRunCustomerRepository = mock(BatchRunCustomerRepository.class);
        txManager = mock(PlatformTransactionManager.class);

        predictionService = new PredictionService(
                fastApiClient
        );

        setField(predictionService, "mapper", mapper);
        setField(predictionService, "customerRepository", customerRepository);
        setField(predictionService, "customerStatusRepository", customerStatusRepository);
        setField(predictionService, "accountRepository", accountRepository);
        setField(predictionService, "predictionRepository", predictionRepository);
        setField(predictionService, "customerTransactionRepository", customerTransactionRepository);
        setField(predictionService, "customerSessionRepository", customerSessionRepository);
        setField(predictionService, "llmClient", llmClient);
        setField(predictionService, "batchRunRepository", batchRunRepository);
        setField(predictionService, "batchRunCustomerRepository", batchRunCustomerRepository);
        setField(predictionService, "txManager", txManager);
    }

    // ============================
    // 1) TEST: predictIntegrationFromDbPro
    // ============================
    @Test
    void predictIntegrationFromDbPro_when_no_existing_prediction_should_call_fastapi_and_persist() {

        LocalDate refDate = LocalDate.of(2026, 1, 15);
        LocalDate bucketDate = refDate.withDayOfMonth(1);

        // Customer mock con lo mínimo que usa buildIntegrationRequestFromDb()
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(1L);
        when(customer.getCustomerId()).thenReturn("CUST-1");
        when(customer.getSurname()).thenReturn("Doe");
        when(customer.getGeography()).thenReturn("France");
        when(customer.getAge()).thenReturn(30);
        when(customer.getEstimatedSalary()).thenReturn(null);
        when(customer.getGender()).thenReturn(null);
        when(customer.getTenure(any(LocalDate.class))).thenReturn(12);

        when(customerRepository.findByCustomerIdIgnoreCase("CUST-1"))
                .thenReturn(Optional.of(customer));

        // No existe predicción previa del bucket
        when(predictionRepository.findByCustomerIdAndPredictionDateFetchCustomer(1L, bucketDate))
                .thenReturn(Optional.empty());

        // Datos DB requeridos por buildIntegrationRequestFromDb()
        when(customerRepository.CountBalanceByCostumerId(1L)).thenReturn(0f);
        when(customerRepository.CountProductsByCostumerId(1L)).thenReturn(1);

        CustomerStatus status = mock(CustomerStatus.class);
        when(status.getCreditScore()).thenReturn(650);
        when(status.getIsActiveMember()).thenReturn(true);
        when(status.getHasCrCard()).thenReturn(true);
        when(customerRepository.findStatusByCustomerId(1L)).thenReturn(status);

        when(customerTransactionRepository.findByCustomerId(1L)).thenReturn(List.of());
        when(customerSessionRepository.findByCustomerId(1L)).thenReturn(List.of());

        // FastAPI integration response
        DataIntegrationResponse apiRes = new DataIntegrationResponse(
                "CUST-1",
                0.87f,
                1,
                "VIP",
                "HIGH",
                null,
                "OK"
        );
        when(fastApiClient.predictIntegration(any(DataIntegrationRequest.class)))
                .thenReturn(Mono.just(apiRes));

        // LLM devuelve JSON válido (para que aiInsightStatus quede OK)
        when(llmClient.generateInsight(anyString()))
                .thenReturn("{\"analisis_breve\":\"ok\",\"estrategia\":{\"semana_1\":\"a\",\"semana_2\":\"b\",\"semana_3\":\"c\",\"semana_4\":\"d\"},\"canal_sugerido\":\"Email\",\"incentivo_recomendado\":\"X\"}");

        // predictionRepository.save no debe romper
        when(predictionRepository.save(any(Prediction.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(predictionService.predictIntegrationFromDbPro("CUST-1", refDate))
                .assertNext(res -> {
                    assertEquals("CUST-1", res.customerId());
                    assertEquals(1, res.predictedLabel());
                    assertNotNull(res.predictedProba());
                })
                .verifyComplete();

        // Verifica que persistió
        verify(predictionRepository, atLeastOnce()).save(any(Prediction.class));
        verify(fastApiClient, times(1)).predictIntegration(any(DataIntegrationRequest.class));
    }

    // ============================
    // 2) TEST: predictIntegrationBatchProAll
    // ============================
    @Test
    void predictIntegrationBatchProAll_when_no_existing_run_should_create_run_and_return_predictions() {

        LocalDate expectedBucket = LocalDate.now().withDayOfMonth(1);

        // 2 customers en DB
        Customer c1 = mock(Customer.class);
        when(c1.getId()).thenReturn(1L);
        when(c1.getCustomerId()).thenReturn("C1");
        when(c1.getSurname()).thenReturn("A");
        when(c1.getGeography()).thenReturn("France");
        when(c1.getAge()).thenReturn(30);
        when(c1.getEstimatedSalary()).thenReturn(null);
        when(c1.getGender()).thenReturn(null);
        when(c1.getTenure(any(LocalDate.class))).thenReturn(10);

        Customer c2 = mock(Customer.class);
        when(c2.getId()).thenReturn(2L);
        when(c2.getCustomerId()).thenReturn("C2");
        when(c2.getSurname()).thenReturn("B");
        when(c2.getGeography()).thenReturn("Spain");
        when(c2.getAge()).thenReturn(40);
        when(c2.getEstimatedSalary()).thenReturn(null);
        when(c2.getGender()).thenReturn(null);
        when(c2.getTenure(any(LocalDate.class))).thenReturn(20);

        when(customerRepository.findAll()).thenReturn(List.of(c1, c2));

        // Datos DB para buildIntegrationRequestFromDb()
        when(customerRepository.CountBalanceByCostumerId(1L)).thenReturn(0f);
        when(customerRepository.CountProductsByCostumerId(1L)).thenReturn(1);
        when(customerRepository.CountBalanceByCostumerId(2L)).thenReturn(0f);
        when(customerRepository.CountProductsByCostumerId(2L)).thenReturn(1);

        CustomerStatus st1 = mock(CustomerStatus.class);
        when(st1.getCreditScore()).thenReturn(600);
        when(st1.getIsActiveMember()).thenReturn(true);
        when(st1.getHasCrCard()).thenReturn(true);

        CustomerStatus st2 = mock(CustomerStatus.class);
        when(st2.getCreditScore()).thenReturn(700);
        when(st2.getIsActiveMember()).thenReturn(true);
        when(st2.getHasCrCard()).thenReturn(false);

        when(customerRepository.findStatusByCustomerId(1L)).thenReturn(st1);
        when(customerRepository.findStatusByCustomerId(2L)).thenReturn(st2);

        when(customerTransactionRepository.findByCustomerId(anyLong())).thenReturn(List.of());
        when(customerSessionRepository.findByCustomerId(anyLong())).thenReturn(List.of());

        // No existe corrida previa (cache)
        when(batchRunRepository.findByBucketDateAndBatchHash(any(LocalDate.class), anyString()))
                .thenReturn(Optional.empty());

        // Persist batch upsert + predicciones:
        // El método real persistBatchUpsertAndPredictions(batch) guarda predicciones, pero en unit test
        // no lo queremos ejecutar. Lo “saltamos” haciendo SPY y stub del método protected.
        PredictionService spy = Mockito.spy(predictionService);
        doReturn(List.<DataIntegrationResponse>of())
                .when(spy)
                .persistBatchUpsertAndPredictions(anyList());

        // Stats batch_stats
        when(fastApiClient.predictBatchStats(anyList()))
                .thenReturn(Mono.just(Map.of("total", 2, "highRisk", 1)));

        // Guardado BatchRun: necesitamos que tenga ID para BatchRunCustomer
        when(batchRunRepository.save(any(BatchRun.class))).thenAnswer(inv -> {
            BatchRun run = inv.getArgument(0);
            // Si tu entidad no tiene setId público, este test te lo dirá y lo ajustamos
            run.setId(123L);
            return run;
        });

        // Predicciones que el service lee al final para responder
        Prediction p1 = mock(Prediction.class);
        when(p1.getCustomer()).thenReturn(c1);
        when(p1.getPredictedProba()).thenReturn(0.9);
        when(p1.getPredictedLabel()).thenReturn(1);
        when(p1.getCustomerSegment()).thenReturn("VIP");
        when(p1.getAiInsight()).thenReturn(null);
        when(p1.getAiInsightStatus()).thenReturn("OK");
        when(p1.getInterventionPriority()).thenReturn("");

        Prediction p2 = mock(Prediction.class);
        when(p2.getCustomer()).thenReturn(c2);
        when(p2.getPredictedProba()).thenReturn(0.1);
        when(p2.getPredictedLabel()).thenReturn(0);
        when(p2.getCustomerSegment()).thenReturn("LOW");
        when(p2.getAiInsight()).thenReturn(null);
        when(p2.getAiInsightStatus()).thenReturn("OK");
        when(p2.getInterventionPriority()).thenReturn("");

        when(predictionRepository.findByBucketDateAndCustomerIdsFetchCustomer(eq(expectedBucket), anyList()))
                .thenReturn(List.of(p1, p2));

        StepVerifier.create(spy.predictIntegrationBatchProAll(LocalDate.of(2026, 1, 15)))
                .assertNext(res -> {
                    assertNotNull(res);
                    assertEquals(expectedBucket, res.bucketDate());
                    assertEquals(123L, res.batchRunId());
                    assertNotNull(res.batchHash());
                    assertEquals(2, ((Number) res.stats().get("total")).intValue());
                    assertEquals(2, res.predictions().size());
                })
                .verifyComplete();

        verify(batchRunRepository, times(1)).save(any(BatchRun.class));
        verify(batchRunCustomerRepository, atLeastOnce()).save(any(BatchRunCustomer.class));
        verify(fastApiClient, times(1)).predictBatchStats(anyList());
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
