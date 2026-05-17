package com.owlite.worker.processor;

import com.owlite.worker.model.ScanJob;

public class ScanJobProcessor implements JobProcessor {
    @Override
    public void process(ScanJob job) {
        String to = (String) job.domainName();
        System.out.printf("[ScanJob] %s → starting scan for %s%n", job.scanId(), to);
        // plug in your actual email logic here
    }
}