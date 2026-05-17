package com.owlite.worker.processor;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.owlite.worker.model.Finding;
import com.owlite.worker.model.ScanJob;
import okhttp3.*;

import java.util.List;
import java.util.Map;

public class AiEnricher {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";
    private final OkHttpClient http = new OkHttpClient();
    private final String apiKey = System.getenv("GROQ_API_KEY");
    private final ObjectMapper mapper = JsonMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .build();

    public List<Finding> generate(ScanJob job) {
        try {
            String prompt = buildGeneratorPrompt(job);

            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "max_tokens", 1500,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)));

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
                System.out.printf("Groq findings response [%d]%n", response.code());

                if (!response.isSuccessful()) {
                    System.err.println("Groq API error: " + response.code() + " " + responseBody);
                    return List.of();
                }

                Map<?, ?> parsed = mapper.readValue(responseBody, Map.class);
                List<?> choices = (List<?>) parsed.get("choices");
                Map<?, ?> first = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) first.get("message");
                String content = (String) message.get("content");

                // strip markdown code fences if present
                content = content.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();

                Finding[] findings = mapper.readValue(content, Finding[].class);
                return List.of(findings);
            }
        } catch (Exception e) {
            System.err.println("Failed to generate findings: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

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

    private String buildGeneratorPrompt(ScanJob job) {
        return String.format("""
            You are a security scanner. Generate exactly 4 realistic but fictional vulnerability findings
            for the domain "%s". Return ONLY a valid JSON array, no explanation, no markdown, no code fences.

            Rules:
            - surface must be one of: Dns, Ssl, HttpHeaders
            - severity must be one of: Critical, High, Medium, Low
            - include at least one finding per surface type
            - cveId can be null or a realistic CVE id like "CVE-2023-1234"
            - all string fields must be non-null except cveId
            - scanId must be "%s" for all findings

            Return this exact shape:
            [
              {
                "scanId": "%s",
                "surface": "Ssl",
                "severity": "High",
                "title": "Weak TLS version detected",
                "cveId": "CVE-2021-3711",
                "aiExplanation": "The server supports TLS 1.0 which is deprecated...",
                "technicalPayload": "TLS version: 1.0, Cipher: RC4-SHA",
                "remediationSteps": "Disable TLS 1.0 and 1.1. Enforce TLS 1.2 minimum."
              }
            ]
            """,
            job.domainName(),
            job.scanId(),
            job.scanId()
        );
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