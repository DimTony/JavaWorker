package com.owlite.worker.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlite.worker.model.ScanJob;
import okhttp3.*;

import java.util.List;
import java.util.Map;

public class AiEnricher {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant"; // free, fast
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey = System.getenv("GROQ_API_KEY");

    public String describe(ScanJob job) {
        try {
            Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", 200,
                "messages", List.of(
                    Map.of("role", "user", "content", buildPrompt(job))
                )
            );

            Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(
                    mapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();

            try (Response response = http.newCall(request).execute()) {
                String responseBody = response.body().string();
                System.out.printf("Groq response [%d]: %s%n", response.code(), responseBody);

                if (!response.isSuccessful()) {
                    System.err.println("Groq API error: " + response.code());
                    return null;
                }

                Map<?, ?> parsed = mapper.readValue(responseBody, Map.class);
                List<?> choices = (List<?>) parsed.get("choices");
                Map<?, ?> first = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) first.get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            System.err.println("Failed to generate scan description: " + e.getMessage());
            e.printStackTrace();
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