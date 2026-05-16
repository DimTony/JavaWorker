package com.owlite.worker.model;

import java.util.Map;

public record Job(
    String jobId,
    String type,
    Map<String, Object> payload
) {}