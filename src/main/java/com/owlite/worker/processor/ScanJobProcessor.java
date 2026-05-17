package com.owlite.worker.processor;

import com.owlite.worker.model.ScanJob;

public class ScanJobProcessor implements JobProcessor {

    private final AiEnricher aiEnricher = new AiEnricher();

    @Override
    public void process(ScanJob job) {
        System.out.printf("[ScanJob] %s → starting scan for %s%n",
            job.scanId(), job.domainName());

        String description = aiEnricher.describe(job);
        if (description != null) {
            System.out.printf("[ScanJob] %s → %s%n", job.scanId(), description);
        }
    }
}