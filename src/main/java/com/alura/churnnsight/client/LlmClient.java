package com.alura.churnnsight.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class LlmClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${llm.api.key}")
    private String apiKey;

    public LlmClient(WebClient.Builder webClientBuilder,
                     @Value("${llm.api.url}") String apiUrl) {
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .build();
    }

    public String generateInsight(String prompt) {
        try {
            var requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            return webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchangeToMono(resp -> logRawThenExtract(resp))
                    .block();

        } catch (Exception e) {
            System.out.println("LLM ERROR: " + e.getMessage());
            return fallbackJson("LLM_ERROR", "No se pudo generar el análisis en este momento.");
        }
    }

    private Mono<String> logRawThenExtract(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(raw -> {
                    System.out.println("RAW GEMINI status=" + response.statusCode() + " body=" + raw);


                    if (response.statusCode().isError()) {
                        return Mono.just(fallbackJson(
                                "HTTP_" + response.statusCode().value(),
                                raw == null || raw.isBlank() ? "empty body" : raw
                        ));
                    }

                    try {
                        JsonNode root = mapper.readTree(raw);

                        if (root.has("error")) {
                            System.out.println("GEMINI BODY ERROR: " + root.get("error").toString());
                            return Mono.just(fallbackJson("GEMINI_BODY_ERROR", root.get("error").toString()));
                        }

                        JsonNode textNode = root.path("candidates")
                                .path(0)
                                .path("content")
                                .path("parts")
                                .path(0)
                                .path("text");

                        String text = textNode.isMissingNode() ? null : textNode.asText(null);

                        if (text == null || text.isBlank()) {
                            return Mono.just(fallbackJson("EMPTY_TEXT", "No text in candidates[0].content.parts[0].text"));
                        }


                        return Mono.just(normalizeToJsonOnly(text));

                    } catch (Exception ex) {
                        System.out.println("GEMINI PARSE ERROR: " + ex.getMessage());
                        return Mono.just(fallbackJson("PARSE_ERROR", ex.getMessage()));
                    }
                });
    }

    private String normalizeToJsonOnly(String rawText) {
        if (rawText == null) return fallbackJson("EMPTY", "null text");

        String t = rawText.trim();


        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            t = t.replaceFirst("\\s*```$", "");
            t = t.trim();
        }

        int firstObj = t.indexOf('{');
        int lastObj = t.lastIndexOf('}');
        if (firstObj >= 0 && lastObj > firstObj) {
            t = t.substring(firstObj, lastObj + 1).trim();
        }

        try {
            JsonNode node = mapper.readTree(t);
            if (!node.isObject()) {
                return fallbackJson("NOT_OBJECT", "JSON válido pero no es objeto");
            }
            return node.toString(); // JSON válido (minificado)
        } catch (Exception e) {
            return fallbackJson("NOT_JSON", "No se pudo parsear salida del LLM");
        }
    }

    private String fallbackJson(String code, String detail) {
        try {
            return mapper.writeValueAsString(Map.of(
                    "error", code,
                    "detail", detail
            ));
        } catch (Exception e) {
            return "{\"error\":\"" + code + "\",\"detail\":\"" + detail.replace("\"", "'") + "\"}";
        }
    }

}
