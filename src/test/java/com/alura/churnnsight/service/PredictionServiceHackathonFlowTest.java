package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.client.LlmClient;
import com.alura.churnnsight.dto.integration.DataIntegrationRequest;
import com.alura.churnnsight.dto.integration.DataIntegrationResponse;
import com.alura.churnnsight.model.*;
import com.alura.churnnsight.model.enumeration.Gender;
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

        predictionService = new PredictionService(fastApiClient);

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
                "Alta - Ofrecer Incentivo",
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
                    assertEquals("Alta - Ofrecer Incentivo", res.interventionPriority());
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

        // 3 customers en DB (ejemplos reales de Data)
        Customer c1 = mock(Customer.class);
        when(c1.getId()).thenReturn(1L);
        when(c1.getCustomerId()).thenReturn("15701166");
        when(c1.getSurname()).thenReturn("Chinedum");
        when(c1.getGeography()).thenReturn("France");
        when(c1.getAge()).thenReturn(40);
        when(c1.getEstimatedSalary()).thenReturn(38761.609);
        when(c1.getGender()).thenReturn(Gender.Male);
        when(c1.getTenure(any(LocalDate.class))).thenReturn(5);

        Customer c2 = mock(Customer.class);
        when(c2.getId()).thenReturn(2L);
        when(c2.getCustomerId()).thenReturn("15592877");
        when(c2.getSurname()).thenReturn("Wright");
        when(c2.getGeography()).thenReturn("Spain");
        when(c2.getAge()).thenReturn(42);
        when(c2.getEstimatedSalary()).thenReturn(35367.191);
        when(c2.getGender()).thenReturn(Gender.Male);
        when(c2.getTenure(any(LocalDate.class))).thenReturn(9);

        Customer c3 = mock(Customer.class);
        when(c3.getId()).thenReturn(3L);
        when(c3.getCustomerId()).thenReturn("15686219");
        when(c3.getSurname()).thenReturn("Wan");
        when(c3.getGeography()).thenReturn("France");
        when(c3.getAge()).thenReturn(38);
        when(c3.getEstimatedSalary()).thenReturn(2444.29);
        when(c3.getGender()).thenReturn(Gender.Male);
        when(c3.getTenure(any(LocalDate.class))).thenReturn(4);

        when(customerRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        // Datos DB para buildIntegrationRequestFromDb()
        when(customerRepository.CountBalanceByCostumerId(1L)).thenReturn(0f);
        when(customerRepository.CountProductsByCostumerId(1L)).thenReturn(1);
        when(customerRepository.CountBalanceByCostumerId(2L)).thenReturn(0f);
        when(customerRepository.CountProductsByCostumerId(2L)).thenReturn(1);
        when(customerRepository.CountBalanceByCostumerId(3L)).thenReturn(0f);
        when(customerRepository.CountProductsByCostumerId(3L)).thenReturn(1);

        CustomerStatus st1 = mock(CustomerStatus.class);
        when(st1.getCreditScore()).thenReturn(600);
        when(st1.getIsActiveMember()).thenReturn(true);
        when(st1.getHasCrCard()).thenReturn(true);

        CustomerStatus st2 = mock(CustomerStatus.class);
        when(st2.getCreditScore()).thenReturn(700);
        when(st2.getIsActiveMember()).thenReturn(true);
        when(st2.getHasCrCard()).thenReturn(false);

        CustomerStatus st3 = mock(CustomerStatus.class);
        when(st3.getCreditScore()).thenReturn(650);
        when(st3.getIsActiveMember()).thenReturn(true);
        when(st3.getHasCrCard()).thenReturn(true);

        when(customerRepository.findStatusByCustomerId(1L)).thenReturn(st1);
        when(customerRepository.findStatusByCustomerId(2L)).thenReturn(st2);
        when(customerRepository.findStatusByCustomerId(3L)).thenReturn(st3);

        when(customerTransactionRepository.findByCustomerId(anyLong())).thenReturn(List.of());
        when(customerSessionRepository.findByCustomerId(anyLong())).thenReturn(List.of());

        // No existe corrida previa (cache)
        when(batchRunRepository.findByBucketDateAndBatchHash(any(LocalDate.class), anyString()))
                .thenReturn(Optional.empty());

        // Persist batch upsert + predicciones:
        // No ejecutamos el método real, lo stubbeamos en SPY
        PredictionService spy = Mockito.spy(predictionService);
        doReturn(List.<DataIntegrationResponse>of())
                .when(spy)
                .persistBatchUpsertAndPredictions(anyList());

        // Stats batch_stats (ajustado a total 3)
        when(fastApiClient.predictBatchStats(anyList()))
                .thenReturn(Mono.just(Map.of("total", 3, "highRisk", 2)));

        // Guardado BatchRun: necesitamos que tenga ID para BatchRunCustomer
        when(batchRunRepository.save(any(BatchRun.class))).thenAnswer(inv -> {
            BatchRun run = inv.getArgument(0);
            run.setId(123L);
            return run;
        });

        // Predicciones que el service lee al final para responder (3 ejemplos reales de Data)
        Prediction p1 = mock(Prediction.class);
        when(p1.getCustomer()).thenReturn(c1);
        when(p1.getPredictedProba()).thenReturn(3.7);
        when(p1.getPredictedLabel()).thenReturn(0);
        when(p1.getCustomerSegment()).thenReturn("Valioso - Bajo compromiso");
        when(p1.getAiInsight()).thenReturn(null);
        when(p1.getAiInsightStatus()).thenReturn("OK");
        when(p1.getInterventionPriority()).thenReturn("Baja - Mantener Contento");

        Prediction p2 = mock(Prediction.class);
        when(p2.getCustomer()).thenReturn(c2);
        when(p2.getPredictedProba()).thenReturn(49.97);
        when(p2.getPredictedLabel()).thenReturn(1);
        when(p2.getCustomerSegment()).thenReturn("Standard");
        when(p2.getAiInsight()).thenReturn(null);
        when(p2.getAiInsightStatus()).thenReturn("OK");
        when(p2.getInterventionPriority()).thenReturn("Media - Monitorear");

        Prediction p3 = mock(Prediction.class);
        when(p3.getCustomer()).thenReturn(c3);
        when(p3.getPredictedProba()).thenReturn(95.12999725341797);
        when(p3.getPredictedLabel()).thenReturn(1);
        when(p3.getCustomerSegment()).thenReturn("Standard");
        when(p3.getAiInsight()).thenReturn(null);
        when(p3.getAiInsightStatus()).thenReturn("OK");
        when(p3.getInterventionPriority()).thenReturn("Alta - Ofrecer Incentivo");

        when(predictionRepository.findByBucketDateAndCustomerIdsFetchCustomer(eq(expectedBucket), anyList()))
                .thenReturn(List.of(p1, p2, p3));

        StepVerifier.create(spy.predictIntegrationBatchProAll(LocalDate.of(2026, 1, 15)))
                .assertNext(res -> {
                    assertNotNull(res);
                    assertEquals(expectedBucket, res.bucketDate());
                    assertEquals(123L, res.batchRunId());
                    assertNotNull(res.batchHash());
                    assertEquals(3, ((Number) res.stats().get("total")).intValue());
                    assertEquals(3, res.predictions().size());
                })
                .verifyComplete();

        verify(batchRunRepository, times(1)).save(any(BatchRun.class));
        verify(batchRunCustomerRepository, atLeastOnce()).save(any(BatchRunCustomer.class));
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
