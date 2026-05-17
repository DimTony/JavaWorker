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
                            Map.of("role", "user", "content", buildPrompt(job))));

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
        return String.format(
                """
                        You are a cybersecurity assistant helping a user understand a vulnerability scan they just initiated.

                        Generate a short but informative response (2–4 sentences) that:
                        - sounds natural and professional
                        - explains what the scan is doing in plain English
                        - briefly mentions what kinds of issues may be checked
                        - reassures the user that results will be available after analysis
                        - avoids heavy technical jargon
                        - does NOT invent findings or vulnerabilities
                        - does NOT use bullet points

                        Scan Details:
                        - Domain: %s
                        - Scan type: %s
                        - Requested at: %s
                        - Scan ID: %s

                        The response should feel intelligent, helpful, and conversational, like a modern AI security platform.

                        Example style:
                        "Your security scan for tonydim.site is now underway. The system will analyze the domain for potential weaknesses, exposed services, and common security risks associated with the selected scan type. Once the analysis is complete, you'll receive a detailed breakdown of any findings and recommended next steps."

                        Generate the response now.
                        """,
                job.domainName(),
                job.scanType(),
                job.enqueuedAt(),
                job.scanId());
    }
}