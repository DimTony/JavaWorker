package com.owlite.worker.processor;

import java.util.List;

import com.owlite.worker.model.Finding;
import com.owlite.worker.model.ScanJob;
import com.owlite.worker.model.ScanResult;
import com.owlite.worker.publisher.ScanResultPublisher;
import com.owlite.worker.repository.FindingPersistenceService;

public class ScanJobProcessor implements JobProcessor {

    private final AiEnricher aiEnricher = new AiEnricher();
    private final FindingPersistenceService persistence = new FindingPersistenceService();
    private final ScanResultPublisher publisher = new ScanResultPublisher();

    @Override
    public void process(ScanJob job) {
        System.out.printf("[ScanJob] %s → starting scan for %s%n",
            job.scanId(), job.domainName());

        String description = aiEnricher.describe(job);
        if (description != null) {
            System.out.printf("[ScanJob] %s → %s%n", job.scanId(), description);
        }

        List<Finding> findings = aiEnricher.generate(job);
        if (findings.isEmpty()) {
            System.err.println("No findings generated for scan " + job.scanId());
            return;
        }

        // 3. Compute a simple security score based on severity
        int score = computeScore(findings);

        // 4. Persist findings + update scan status
        persistence.saveFindings(job.scanId(), findings, score);

        // 5. Publish result to Redis for .NET to pick up
        publisher.publish(new ScanResult(
            job.scanId(),
            job.domainId(),
            job.domainName(),
            job.requestedBy(),
            score,
            findings
        ));
    }

    private int computeScore(List<Finding> findings) {
        int deductions = findings.stream().mapToInt(f -> switch (f.severity()) {
            case "Critical" -> 30;
            case "High"     -> 20;
            case "Medium"   -> 10;
            case "Low"      ->  5;
            default         ->  0;
        }).sum();

        return Math.max(0, 100 - deductions);
    }
}