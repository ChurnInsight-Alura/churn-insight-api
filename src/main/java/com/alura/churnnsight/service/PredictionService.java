package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.client.LlmClient;
import com.alura.churnnsight.dto.BatchProResponse;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.dto.consult.DataPredictionDetail;
import com.alura.churnnsight.dto.integration.*;

import com.alura.churnnsight.exception.BusinessException;
import com.alura.churnnsight.exception.DownstreamException;
import com.alura.churnnsight.exception.NotFoundException;
import com.alura.churnnsight.model.*;
import com.alura.churnnsight.model.enumeration.InterventionPriority;
import com.alura.churnnsight.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.alura.churnnsight.service.helpers.BirthDateEstimator;
import com.alura.churnnsight.service.helpers.GenderMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

import java.util.Map;
import java.util.stream.Collectors;


@Service
public class PredictionService {

    private final FastApiClient fastApiClient;

    @Autowired
    private ObjectMapper mapper;

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

    @Autowired
    private BatchRunRepository batchRunRepository;

    @Autowired
    private BatchRunCustomerRepository batchRunCustomerRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private TransactionTemplate txTemplate;

    public PredictionService(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }

    @PostConstruct
    void initTxTemplate() {
        this.txTemplate = new TransactionTemplate(txManager);
    }

    // EXISTENTE: predicción 1 cliente desde DB
    public Mono<DataPredictionResult> predictForCustomer(String customerId) {
        return Mono.fromCallable(() -> predictAndPersist(customerId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    protected DataPredictionResult predictAndPersist(String customerId) {
        LocalDate refDate = LocalDate.now();

        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

        Long id = customer.getId();

        int tenureMonths = customer.getTenure(refDate);

        int isActiveMember = customer.getStatus() != null && Boolean.TRUE.equals(customer.getStatus().getIsActiveMember()) ? 1 : 0;

        Float balanceDb = customerRepository.CountBalanceByCostumerId(id);
        float balance = (balanceDb == null) ? 0f : balanceDb;

        Integer numProductsDb = customerRepository.CountProductsByCostumerId(id);
        int numProducts = (numProductsDb == null) ? 0 : numProductsDb;

        DataMakePrediction data = new DataMakePrediction(
                customer,
                tenureMonths,
                balance,
                numProducts,
                isActiveMember
        );

        DataPredictionResult response = fastApiClient.predict(data).block();
        if (response == null) {
            throw new DownstreamException("FASTAPI", 502, "Null response from /predict");
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
        System.out.println("tenure"+tenureMonths);
        System.out.println("balance"+balance);
        System.out.println("numofproduct"+numProductsDb);
        return response;
    }

    public Page<DataPredictionDetail> getPredictionsByCustomerId(String customerId, Pageable pageable) {
        Customer customer = customerRepository
                .findByCustomerIdIgnoreCase(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

        return predictionRepository.findByCustomerId(customer.getId(), pageable)
                .map(DataPredictionDetail::new);
    }

    // EXISTENTE: integration request directo (no persiste)

    public Mono<DataIntegrationResponse> predictIntegration(DataIntegrationRequest request) {
        return fastApiClient.predictIntegration(normalize(request));
    }

    // EXISTENTE: integration desde DB + persistencia

    public Mono<List<DataIntegrationResponse>> predictIntegrationBatch(List<DataIntegrationRequest> requests) {
        return fastApiClient.predictBatch(requests);
    }

    // HELPERS IA

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

    private LocalDate getMonthBucket(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    // Integration BATCH + UPSERT + PERSIST + HELPERS
    public Mono<List<DataIntegrationResponse>> predictIntegrationBatchUpsertAndPersist(List<DataIntegrationRequest> batch) {
        return Mono.fromCallable(() -> persistBatchUpsertAndPredictions(batch))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    protected List<DataIntegrationResponse> persistBatchUpsertAndPredictions(List<DataIntegrationRequest> batch) {

        if (batch == null || batch.isEmpty()) return List.of();

        // 1) Llamar a Data (FastAPI)
        List<DataIntegrationResponse> responses = fastApiClient.predictBatch(batch).block();
        if (responses == null)    throw new DownstreamException("FASTAPI", 502, "Null response from /predict/batch");

        // 2) Mapa request por customerId (robusto si se desordena)
        Map<String, DataIntegrationRequest> reqByCustomerId = batch.stream()
                .filter(r -> r != null && r.cliente() != null && r.cliente().customerId() != null)
                .collect(Collectors.toMap(
                        r -> r.cliente().customerId(),
                        r -> r,
                        (a, b) -> a
                ));

        LocalDate bucketDate = getMonthBucket(LocalDate.now());
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
            prediction.setInterventionPriority(res.interventionPriority());

            // 3.5 LLM en batch
            if (shouldGenerateInsight(prediction)) {
                String prompt = buildRetentionPlanPrompt(req, res);
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
                    .findByCustomerIdAndTransactionId(customer.getId(),tx.transactionId())
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
                    .findByCustomerIdAndSessionId(customer.getId(),s.sessionId())
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

    private String computeBatchHash(LocalDate bucketDate, List<String> customerIds) {
        try {
            List<String> sorted = customerIds.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();

            String base = bucketDate + "|" + String.join(",", sorted);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);

        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute batch hash", e);
        }
    }

    public Mono<DataIntegrationResponse> predictIntegrationFromDbPro(String customerId, LocalDate refDate) {
        System.out.println("Enviando Body: ");
        LocalDate effectiveRefDate = (refDate != null) ? refDate : LocalDate.now();
        LocalDate bucketDate = getMonthBucket(effectiveRefDate);

        return Mono.fromCallable(() -> {

            Customer customer = customerRepository
                    .findByCustomerIdIgnoreCase(customerId)
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

            // 1) si ya existe para bucket => devolver desde DB (PERO completa LLM si falta)
            var existingOpt = predictionRepository.findByCustomerIdAndPredictionDateFetchCustomer(customer.getId(), bucketDate);

            if (existingOpt.isPresent()) {
                Prediction p = existingOpt.get();

                // Si existe pero NO tiene insight, generarlo y actualizar
                if (shouldGenerateInsight(p)) {

                    // Necesitamos el request para construir el prompt (reusa tu lógica)
                    DataIntegrationRequest req = buildIntegrationRequestFromDb(customer, effectiveRefDate);

                    // Res "similar" para el prompt (no cambia churn)
                    DataIntegrationResponse pseudoRes = new DataIntegrationResponse(
                            customer.getCustomerId(),
                            (float) p.getPredictedProba(),
                            p.getPredictedLabel(),
                            p.getCustomerSegment(),
                            p.getInterventionPriority() != null ? p.getInterventionPriority().toString() : null,
                            null,
                            null
                    );

                    String prompt = buildRetentionPlanPrompt(req, pseudoRes);

                    String insight;
                    try {
                        insight = llmClient.generateInsight(prompt);
                    } catch (Exception e) {
                        insight = null;
                    }

                    String stored = normalizeInsightForStorage(insight);
                    p.setAiInsight(stored);
                    p.setAiInsightStatus(classifyAiInsightStatus(stored));

                    predictionRepository.save(p);
                }

                return toIntegrationResponse(p);
            }

            // 2) si no existe => generar + persistir
            DataIntegrationRequest req = buildIntegrationRequestFromDb(customer, effectiveRefDate);

            DataIntegrationResponse res = fastApiClient.predictIntegration(req).block();
            if (res == null) throw new DownstreamException("FASTAPI", 502, "Null response from /predict/integration");

            Prediction p = new Prediction();
            p.setCustomer(customer);
            p.setPredictionDate(bucketDate);
            p.setPredictedProba(res.predictedProba());
            p.setPredictedLabel(res.predictedLabel());
            p.setCustomerSegment(res.customerSegment());
            p.setInterventionPriority(res.interventionPriority());

            // LLM siempre se intenta aquí (si falla, se guarda error/missing)
            String prompt = buildRetentionPlanPrompt(req, res);
            String insight;
            try { insight = llmClient.generateInsight(prompt); } catch (Exception e) { insight = null; }

            String stored = normalizeInsightForStorage(insight);
            p.setAiInsight(stored);
            p.setAiInsightStatus(classifyAiInsightStatus(stored));

            predictionRepository.save(p);

            return toIntegrationResponse(p);

        }).subscribeOn(Schedulers.boundedElastic());
    }

    private DataIntegrationRequest buildIntegrationRequestFromDb(Customer customer, LocalDate refDate) {

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

        return new DataIntegrationRequest(
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
    }

    private DataIntegrationResponse toIntegrationResponse(Prediction p) {
        return new DataIntegrationResponse(
                p.getCustomer().getCustomerId(),
                (float) p.getPredictedProba(),
                p.getPredictedLabel(),
                p.getCustomerSegment(),
                p.getInterventionPriority().toString(),
                parseAiInsight(p.getAiInsight()),
                p.getAiInsightStatus()
        );
    }

    public Mono<BatchProResponse> predictIntegrationBatchPro(List<DataIntegrationRequest> batch) {
        return Mono.fromCallable(() ->
                txTemplate.execute(status -> runBatchPro(batch))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<BatchProResponse> predictIntegrationBatchProAll(LocalDate refDate) {
        LocalDate effectiveRefDate = (refDate != null) ? refDate : LocalDate.now();

        return Mono.fromCallable(() -> {

            // 1) Traer todos los customers registrados
            List<Customer> customers = customerRepository.findAll();

            if (customers.isEmpty()) {
                throw new BusinessException("NO_CUSTOMERS", "No hay clientes registrados para procesar.");
            }

            // 2) Armar batch request desde BD
            List<DataIntegrationRequest> batch = customers.stream()
                    .map(c -> buildIntegrationRequestFromDb(c, effectiveRefDate))
                    .toList();

            // 3) Reusar tu batch pro (persist + stats + cache)
            return runBatchPro(batch);

        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    protected BatchProResponse runBatchPro(List<DataIntegrationRequest> batch) {

        if (batch == null || batch.isEmpty()) {
            return new BatchProResponse(getMonthBucket(LocalDate.now()), null, null, Map.of(), List.of());
        }

        LocalDate bucketDate = getMonthBucket(LocalDate.now());

        // customerIds del payload
        List<String> customerIds = batch.stream()
                .filter(b -> b != null && b.cliente() != null && b.cliente().customerId() != null)
                .map(b -> b.cliente().customerId())
                .toList();

        String batchHash = computeBatchHash(bucketDate, customerIds);

        // 1) Si ya existe corrida => devolver desde DB (NO recalcular)
        var existingRunOpt = batchRunRepository.findByBucketDateAndBatchHash(bucketDate, batchHash);
        if (existingRunOpt.isPresent()) {
            BatchRun run = existingRunOpt.get();
            List<String> ids = batchRunCustomerRepository.findCustomerIdsByBatchRunId(run.getId());

            // traer predicciones guardadas
            List<String> idsLower = ids.stream().map(String::toLowerCase).toList();
            List<Prediction> preds = predictionRepository.findByBucketDateAndCustomerIdsFetchCustomer(bucketDate, idsLower);

            List<DataIntegrationResponse> responses = preds.stream()
                    .map(this::toIntegrationResponse)
                    .toList();

            Map<String, Object> stats = parseStatsJson(run.getStatsJson());

            return new BatchProResponse(bucketDate, run.getId(), batchHash, stats, responses);
        }

        // 2) Si no existe corrida => generar predicciones+LLM+persist (usa tu batch upsert existente)
        // IMPORTANTE: este método debe guardar Prediction con aiInsight y aiInsightStatus
        persistBatchUpsertAndPredictions(batch); // ya lo tienes

        // 3) stats del grupo (FastAPI batch_stats) y persistir
        Map<String, Object> stats = fastApiClient.predictBatchStats(batch).block();
        if (stats == null) stats = Map.of("error", "stats_null");

        // 4) guardar run
        BatchRun run = new BatchRun();
        run.setBucketDate(bucketDate);
        run.setBatchHash(batchHash);
        run.setStatsJson(toJson(stats));
        batchRunRepository.save(run);

        // 5) guardar clientes del run
        for (String cid : customerIds.stream().distinct().toList()) {
            batchRunCustomerRepository.save(new BatchRunCustomer(run.getId(), cid));
        }

        // 6) responder desde DB (con aiInsight)
        List<String> idsLower = customerIds.stream().distinct().map(String::toLowerCase).toList();
        List<Prediction> preds = predictionRepository.findByBucketDateAndCustomerIdsFetchCustomer(bucketDate, idsLower);

        List<DataIntegrationResponse> responses = preds.stream()
                .map(this::toIntegrationResponse)
                .toList();

        return new BatchProResponse(bucketDate, run.getId(), batchHash, stats, responses);
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"SERIALIZE\",\"message\":\"Cannot serialize stats\"}";
        }
    }

    private Map<String, Object> parseStatsJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("error", "PARSE_STATS", "raw", json);
        }
    }

    private Object  parseAiInsight(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            JsonNode node = mapper.readTree(raw);

            if (node.isTextual()) {
                node = mapper.readTree(node.asText());
            }
            return mapper.convertValue(node, Object.class);
        } catch (Exception e) {
            return Map.of("error", "NON_JSON", "message", raw);
        }
    }

    private DataIntegrationRequest normalize(DataIntegrationRequest req) {
        if (req == null || req.cliente() == null) return req;

        ClienteIn c = req.cliente();

        Integer numProducts = (c.numOfProducts() == null) ? 0 : c.numOfProducts();
        Float balance = (c.balance() == null) ? 0f : c.balance();
        Integer isActive = (c.isActiveMember() == null) ? 0 : c.isActiveMember();
        Integer hasCard = (c.hasCrCard() == null) ? 0 : c.hasCrCard();

        ClienteIn fixed = new ClienteIn(
                c.rowNumber(),
                c.customerId(),
                c.surname(),
                c.creditScore(),
                c.geography(),
                c.gender(),
                c.age(),
                c.tenure(),
                balance,
                numProducts,
                hasCard,
                isActive,
                c.estimatedSalary()
        );

        return new DataIntegrationRequest(fixed, req.transacciones(), req.sesiones());
    }


}
