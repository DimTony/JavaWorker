package com.owlite.worker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScanJob(
    @JsonProperty("DomainId")     String domainId,
    @JsonProperty("DomainName")   String domainName,
    @JsonProperty("ScanId")       String scanId,
    @JsonProperty("ScanType")     String scanType,
    @JsonProperty("RequestedBy")  String requestedBy,
    @JsonProperty("EnqueuedAt")   String enqueuedAt
) {}