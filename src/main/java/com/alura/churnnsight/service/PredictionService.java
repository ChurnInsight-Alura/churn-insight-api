package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.client.LlmClient;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.dto.consult.DataPredictionDetail;
import com.alura.churnnsight.dto.integration.*;

import com.alura.churnnsight.model.*;
import com.alura.churnnsight.model.enumeration.InterventionPriority;
import com.alura.churnnsight.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.alura.churnnsight.service.helpers.BirthDateEstimator;
import com.alura.churnnsight.service.helpers.GenderMapper;

import java.time.LocalDate;
import java.util.List;

import java.util.Map;
import java.util.stream.Collectors;


@Service
public class PredictionService {

    private final FastApiClient fastApiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerStatusRepository customerStatusRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;

    @Autowired
    private CustomerSessionRepository customerSessionRepository;

    @Autowired
    private LlmClient llmClient;

    public PredictionService(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }

    // EXISTENTE: predicción 1 cliente desde DB
    public Mono<DataPredictionResult> predictForCustomer(String customerId) {
        return Mono.fromCallable(() -> predictAndPersist(customerId))
                .subscribeOn(Schedulers.boundedElastic());
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

        DataPredictionResult response = fastApiClient.predict(data).block();
        if (response == null) {
            throw new IllegalStateException("Prediction services returned null");
        }

        Prediction prediction = new Prediction(response, customer);

        String prompt = buildRetentionPlanPrompt(data, response);


        if (shouldGenerateInsight(prediction)) {
            String insight;
            try {
                insight = llmClient.generateInsight(prompt);
            } catch (Exception e) {
                insight = null;
            }

            String stored = normalizeInsightForStorage(insight);
            prediction.setAiInsight(stored);
            prediction.setAiInsightStatus(classifyAiInsightStatus(stored));

        } else {
            prediction.setAiInsightStatus("OK");
        }

        predictionRepository.save(prediction);
        return response;
    }

    public Page<DataPredictionDetail> getPredictionsByCustomerId(String customerId, Pageable pageable) {
        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        return predictionRepository.findByCustomerId(customer.getId(), pageable)
                .map(DataPredictionDetail::new);
    }

    // EXISTENTE: integration request directo (no persiste)

    public Mono<DataIntegrationResponse> predictIntegration(DataIntegrationRequest request) {
        return fastApiClient.predictIntegration(request);
    }

    // EXISTENTE: integration desde DB + persistencia

    public Mono<DataIntegrationResponse> predictIntegrationFromDb(String customerId, LocalDate refDate) {
        LocalDate effectiveRefDate = (refDate != null) ? refDate : LocalDate.now();
        return Mono.fromCallable(() -> predictIntegrationFromDbAndPersist(customerId, effectiveRefDate))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<DataIntegrationResponse>> predictIntegrationBatch(List<DataIntegrationRequest> requests) {
        return fastApiClient.predictBatch(requests);
    }

    @Transactional
    protected DataIntegrationResponse predictIntegrationFromDbAndPersist(String customerId, LocalDate refDate) {

        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        Integer rowNumber = customer.getId() == null ? null : customer.getId().intValue();
        int tenureMonths = customer.getTenure(refDate);

        Float balance = customerRepository.CountBalanceByCostumerId(customer.getId());
        float balanceF = (balance == null) ? 0f : balance.floatValue();

        Integer numProductsDb = customerRepository.CountProductsByCostumerId(customer.getId());
        int numProducts = (numProductsDb == null) ? 0 : numProductsDb;

        CustomerStatus st = customerRepository.findStatusByCustomerId(customer.getId());
        Integer creditScore = (st != null) ? st.getCreditScore() : null;
        int isActiveMember = (st != null && Boolean.TRUE.equals(st.getIsActiveMember())) ? 1 : 0;
        int hasCrCard = (st != null && Boolean.TRUE.equals(st.getHasCrCard())) ? 1 : 0;

        String gender = (customer.getGender() == null) ? null : customer.getGender().name();

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
                        customer.getEstimatedSalary() == null ? null : customer.getEstimatedSalary().floatValue()
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

        DataIntegrationResponse response = fastApiClient.predictIntegration(req).block();
        if (response == null) throw new IllegalStateException("Prediction services returned null");

        LocalDate execDate = (refDate != null) ? refDate : LocalDate.now();
        LocalDate bucketDate = getQuincenaBucket(execDate);

        Prediction prediction = predictionRepository
                .findByCustomerIdAndPredictionDate(customer.getId(), bucketDate)
                .orElseGet(Prediction::new);

        prediction.setCustomer(customer);
        prediction.setPredictionDate(bucketDate);
        prediction.setPredictedProba(response.predictedProba());
        prediction.setPredictedLabel(response.predictedLabel());
        prediction.setCustomerSegment(response.customerSegment());
        prediction.setInterventionPriority(
                InterventionPriority.fromDataLabel(response.interventionPriority())
        );

        if (shouldGenerateInsight(prediction)) {
            String prompt = buildRetentionPlanPrompt(req, response);

            String insight;
            try {
                insight = llmClient.generateInsight(prompt);
            } catch (Exception e) {
                insight = null;
            }

            String stored = normalizeInsightForStorage(insight);
            prediction.setAiInsight(stored);
            prediction.setAiInsightStatus(classifyAiInsightStatus(stored));
        }

        predictionRepository.save(prediction);

        return response;
    }

    private String buildRetentionPlanPrompt(Object contextoCliente, Object prediccionModelo) {
        String contextoJson = toPrettyJson(contextoCliente);
        String prediccionJson = toPrettyJson(prediccionModelo);

        return """
                Actúa como un Gerente de Retención de Clientes Senior en un Banco Digital.
                Tu objetivo es crear un plan de recuperación personalizado de 4 semanas para un cliente en riesgo de abandono.
                
                CONTEXTO DEL CLIENTE:
                %s
                
                PREDICCIÓN OBTENIDA POR EL MODELO:
                %s
                
                REGLAS DE NEGOCIO:
                
                Los distintos segmentos de grupos de cliente son:
                  1. 'Poco Valor'
                  2. 'Cliente potencial'
                  3. 'Standard'
                  4. 'Valioso - Bajo compromiso'
                  5. 'VIP'
                
                Los niveles de prioridad de acción, determinados a partir del segmento del cliente y la probabilidad de abandono:
                  1. "Baja - Mantener Contento"
                  2. "Media - Monitorear"
                  3. "Media - Correo Electrónico Automático"
                  4. "Alta - ofrecer incentivo"
                  5. "Alta - Chequeo Personalizado"
                  6. "CRÍTICO - llamar inmediatamente", es valioso y se va a ir
                
                SALIDA REQUERIDA:
                Devuelve SOLAMENTE un objeto JSON válido (sin markdown, sin texto extra) con la siguiente estructura:
                {
                  "analisis_breve": "Una frase breve explicando por qué se quiere ir",
                  "estrategia": {
                    "semana_1": "Acción inmediata de choque",
                    "semana_2": "Seguimiento o incentivo",
                    "semana_3": "Recordatorio de beneficios",
                    "semana_4": "Encuesta de satisfacción o cierre"
                  },
                  "canal_sugerido": "Email | Teléfono | WhatsApp",
                  "incentivo_recomendado": "Ej: Tasa preferencial, Bonificación, etc."
                }
                """.formatted(contextoJson, prediccionJson);
    }

    private String toPrettyJson(Object obj) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private String normalizeInsightForStorage(String insight) {
        if (insight == null || insight.isBlank()) {
            return "{\"error\":\"MISSING\",\"message\":\"No se pudo generar el análisis en este momento.\"}";
        }

        try {
            var node = mapper.readTree(insight);

            if (node.has("error") && node.has("detail")) {
                String err = node.path("error").asText("ERROR");
                String detail = node.path("detail").asText("");

                if (detail.contains("API key not valid")) {
                    return "{\"error\":\"API_KEY_INVALID\",\"message\":\"No se pudo generar el análisis en este momento.\"}";
                }

                return "{\"error\":\"" + err + "\",\"message\":\"No se pudo generar el análisis en este momento.\"}";
            }

            return insight;

        } catch (Exception ignored) {
            String safe = insight.replace("\"", "'");
            return "{\"error\":\"NON_JSON\",\"message\":\"" + safe + "\"}";
        }
    }

    private String classifyAiInsightStatus(String aiInsight) {
        if (aiInsight == null || aiInsight.isBlank()) return "MISSING";

        try {
            var node = mapper.readTree(aiInsight);
            return node.has("error") ? "ERROR" : "OK";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private boolean shouldGenerateInsight(Prediction prediction) {
        String status = prediction.getAiInsightStatus();
        String aiInsight = prediction.getAiInsight();

        if (aiInsight == null || aiInsight.isBlank()) return true;

        if ("ERROR".equalsIgnoreCase(status)) return true;

        if (status == null || status.isBlank()) {
            if (aiInsight.startsWith("No se pudo generar")) return true;
            if (aiInsight.contains("\"error\"")) return true;
        }

        return false;
    }

    private LocalDate getQuincenaBucket(LocalDate date) {
        return (date.getDayOfMonth() <= 15)
                ? date.withDayOfMonth(1)
                : date.withDayOfMonth(16);
    }

    // Integration BATCH + UPSERT + PERSIST
    public Mono<List<DataIntegrationResponse>> predictIntegrationBatchUpsertAndPersist(List<DataIntegrationRequest> batch) {
        return Mono.fromCallable(() -> persistBatchUpsertAndPredictions(batch))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    protected List<DataIntegrationResponse> persistBatchUpsertAndPredictions(List<DataIntegrationRequest> batch) {

        if (batch == null || batch.isEmpty()) return List.of();

        // 1) Llamar a Data (FastAPI)
        List<DataIntegrationResponse> responses = fastApiClient.predictBatch(batch).block();
        if (responses == null) throw new IllegalStateException("Prediction services returned null (batch)");

        // 2) Mapa request por customerId (robusto si se desordena)
        Map<String, DataIntegrationRequest> reqByCustomerId = batch.stream()
                .filter(r -> r != null && r.cliente() != null && r.cliente().customerId() != null)
                .collect(Collectors.toMap(
                        r -> r.cliente().customerId(),
                        r -> r,
                        (a, b) -> a
                ));

        LocalDate bucketDate = getQuincenaBucket(LocalDate.now());
        // 3) Persistir todo por cada respuesta
        for (DataIntegrationResponse res : responses) {

            if (res == null || res.customerId() == null) continue;

            String customerId = res.customerId();
            DataIntegrationRequest req = reqByCustomerId.get(customerId);
            if (req == null) continue;

            ClienteIn c = req.cliente();

            // 3.1 UPSERT customer
            Customer customer = customerRepository
                    .findByCustomerIdIgnoreCase(customerId)
                    .orElseGet(Customer::new);

            customer.setCustomerId(customerId);
            customer.setSurname(c.surname());
            customer.setGeography(c.geography());
            customer.setGender(GenderMapper.parseGender(c.gender()));

            // birthDate desde age (requerido)
            customer.setBirthDate(BirthDateEstimator.fromAge(c.age()));

            customer.setEstimatedSalary(c.estimatedSalary() == null ? null : c.estimatedSalary().doubleValue());

            customerRepository.save(customer);

            // 3.2 UPSERT status + account
            upsertCustomerStatus(customer, c);
            upsertAccount(customer, c);

            //3.3 UPSERT transacciones y sesiones
            upsertTransactions(customer, req.transacciones());
            upsertSessions(customer, req.sesiones());

            // 3.4 UPSERT prediction
            Prediction prediction = predictionRepository
                    .findByCustomerIdAndPredictionDate(customer.getId(), bucketDate)
                    .orElseGet(Prediction::new);

            prediction.setCustomer(customer);
            prediction.setPredictionDate(bucketDate);
            prediction.setPredictedProba(res.predictedProba());
            prediction.setPredictedLabel(res.predictedLabel());
            prediction.setCustomerSegment(res.customerSegment());
            prediction.setInterventionPriority(InterventionPriority.fromDataLabel(res.interventionPriority()));

            predictionRepository.save(prediction);
        }

        return responses;
    }

    private void upsertCustomerStatus(Customer customer, ClienteIn c) {

        CustomerStatus st = customerStatusRepository.findByCustomer(customer)
                .orElseGet(CustomerStatus::new);

        st.setCustomer(customer);
        st.setCreditScore(c.creditScore());

        // En contrato viene Integer 0/1 (o a veces 2 por error).
        st.setHasCrCard(c.hasCrCard() != null && c.hasCrCard() == 1);
        st.setIsActiveMember(c.isActiveMember() != null && c.isActiveMember() == 1);

        // Si tu entidad tiene otros campos (tenure, numProducts), agrégalos aquí si existen.
        customerStatusRepository.save(st);
    }

    private void upsertAccount(Customer customer, ClienteIn c) {
        Account acc = accountRepository.findByCustomer(customer)
                .orElseGet(Account::new);

        acc.setCustomer(customer);
        acc.setBalance(c.balance() == null ? 0.0 : c.balance().doubleValue());

        accountRepository.save(acc);
    }

    private void upsertTransactions(Customer customer, List<TransaccionIn> transacciones) {

        if (transacciones == null || transacciones.isEmpty()) return;

        for (TransaccionIn tx : transacciones) {

            CustomerTransaction entity = customerTransactionRepository
                    .findByTransactionId(tx.transactionId())
                    .orElseGet(CustomerTransaction::new);

            entity.setTransactionId(tx.transactionId());
            entity.setCustomer(customer);
            entity.setTransactionDate(tx.transactionDate());
            entity.setAmount(
                    tx.amount() == null ? 0.0 : tx.amount().doubleValue()
            );
            entity.setTransactionType(tx.transactionType());

            customerTransactionRepository.save(entity);
        }
    }

    private void upsertSessions(Customer customer, List<SesionIn> sesiones) {

        if (sesiones == null || sesiones.isEmpty()) return;

        for (SesionIn s : sesiones) {

            CustomerSession entity = customerSessionRepository
                    .findBySessionId(s.sessionId())
                    .orElseGet(CustomerSession::new);

            entity.setSessionId(s.sessionId());
            entity.setCustomer(customer);
            entity.setSessionDate(s.sessionDate());
            entity.setDurationMin(
                    s.durationMin() == null ? 0.0 : s.durationMin()
            );
            entity.setUsedTransfer(
                    s.usedTransfer() == null ? 0 : s.usedTransfer()
            );
            entity.setUsedPayment(
                    s.usedPayment() == null ? 0 : s.usedPayment()
            );
            entity.setUsedInvest(
                    s.usedInvest() == null ? 0 : s.usedInvest()
            );
            entity.setOpenedPush(
                    s.openedPush() == null ? 0 : s.openedPush()
            );
            entity.setFailedLogin(
                    s.failedLogin() == null ? 0 : s.failedLogin()
            );

            customerSessionRepository.save(entity);
        }
    }

}
