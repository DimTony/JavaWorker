package com.owlite.worker.model;

public record ScanJob(
    String domainId,
    String domainName,
    String scanId,
    String scanType,
    String requestedBy,
    String enqueuedAt
) {}