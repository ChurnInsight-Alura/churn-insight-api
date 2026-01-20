package com.alura.churnnsight.client;

import com.alura.churnnsight.config.FastApiProperties;
import com.alura.churnnsight.dto.integration.DataIntegrationRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FastApiClientHackathonTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void predictIntegration_should_call_predictCustomerPath_and_parse_response() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "CustomerId": "CUST-1",
                      "PredictedProba": 0.77,
                      "PredictedLabel": 1,
                      "CustomerSegment": "VIP",
                      "InterventionPriority": "HIGH",
                      "aiInsight": {"summary":"ok"},
                      "aiInsightStatus": "OK"
                    }
                """));

        String baseUrl = server.url("/").toString();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();

        FastApiProperties props = mock(FastApiProperties.class);
        when(props.getPredictCustomerPath()).thenReturn("/predict");
        when(props.getTimeoutSeconds()).thenReturn(5);

        FastApiClient client = new FastApiClient(webClient, props);

        DataIntegrationRequest req = new DataIntegrationRequest(null, List.of(), List.of());

        StepVerifier.create(client.predictIntegration(req))
                .assertNext(res -> {
                    assertEquals("CUST-1", res.customerId());
                    assertEquals(1, res.predictedLabel());
                    assertEquals("OK", res.aiInsightStatus());
                })
                .verifyComplete();

        var recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/predict", recorded.getPath());
    }

    @Test
    void predictBatch_should_call_predictBatchPath_and_parse_list() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                    [
                      {
                        "CustomerId":"C1",
                        "PredictedProba":0.9,
                        "PredictedLabel":1,
                        "CustomerSegment":"VIP",
                        "InterventionPriority":"HIGH",
                        "aiInsight":{"summary":"a"},
                        "aiInsightStatus":"OK"
                      },
                      {
                        "CustomerId":"C2",
                        "PredictedProba":0.1,
                        "PredictedLabel":0,
                        "CustomerSegment":"LOW",
                        "InterventionPriority":"LOW",
                        "aiInsight":{"summary":"b"},
                        "aiInsightStatus":"OK"
                      }
                    ]
                """));

        String baseUrl = server.url("/").toString();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();

        FastApiProperties props = mock(FastApiProperties.class);
        when(props.getPredictBatchPath()).thenReturn("/predict/batch");
        when(props.getTimeoutSeconds()).thenReturn(5);

        FastApiClient client = new FastApiClient(webClient, props);

        List<DataIntegrationRequest> batchReq = List.of(
                new DataIntegrationRequest(null, List.of(), List.of()),
                new DataIntegrationRequest(null, List.of(), List.of())
        );

        StepVerifier.create(client.predictBatch(batchReq))
                .assertNext(list -> {
                    assertEquals(2, list.size());
                    assertEquals("C1", list.get(0).customerId());
                })
                .verifyComplete();

        var recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/predict/batch", recorded.getPath());
    }
}

