package com.owlite.worker.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlite.worker.model.ScanJob;
import okhttp3.*;

import java.util.List;
import java.util.Map;

public class AiEnricher {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001"; // cheapest
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey = System.getenv("ANTHROPIC_API_KEY");

    public String describe(ScanJob job) {
        try {
            String prompt = buildPrompt(job);

            Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", 200,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(
                    mapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .build();

            try (Response response = http.newCall(request).execute()) {
                String responseBody = response.body().string();
                Map<?, ?> parsed = mapper.readValue(responseBody, Map.class);
                List<?> content = (List<?>) parsed.get("content");
                Map<?, ?> first = (Map<?, ?>) content.get(0);
                return (String) first.get("text");
            }
        } catch (Exception e) {
            System.err.println("Failed to generate scan description: " + e.getMessage());
            return null;
        }
    }

    private String buildPrompt(ScanJob job) {
        return String.format("""
            Write a single friendly sentence describing this vulnerability scan to the user who requested it.
            Be concise and human-readable. Do not use technical jargon.

            Details:
            - Domain: %s
            - Scan type: %s
            - Requested at: %s
            - Scan ID: %s

            Example: "You have initiated a scan for tonydim.site, requested on 17th May 2026 at 10:11 AM."
            """,
            job.domainName(),
            job.scanType(),
            job.enqueuedAt(),
            job.scanId()
        );
    }
}