package com.owlite.worker.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlite.worker.model.ScanJob;
import okhttp3.*;

import java.util.List;
import java.util.Map;

public class AiEnricher {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey = System.getenv("GEMINI_API_KEY");

    public String describe(ScanJob job) {
        try {
            Map<String, Object> body = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", buildPrompt(job))
                    ))
                )
            );

            String url = API_URL + "?key=" + apiKey;

            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(
                    mapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            try (Response response = http.newCall(request).execute()) {
                String responseBody = response.body().string();
                System.out.printf("Gemini response [%d]: %s%n", response.code(), responseBody);

                if (!response.isSuccessful()) {
                    System.err.println("Gemini API error: " + response.code());
                    return null;
                }

                Map<?, ?> parsed = mapper.readValue(responseBody, Map.class);
                List<?> candidates = (List<?>) parsed.get("candidates");
                Map<?, ?> first = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) first.get("content");
                List<?> parts = (List<?>) content.get("parts");
                Map<?, ?> part = (Map<?, ?>) parts.get(0);
                return (String) part.get("text");
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