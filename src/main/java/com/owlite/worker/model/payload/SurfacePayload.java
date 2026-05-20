package com.owlite.worker.model.payload;

public sealed interface SurfacePayload
    permits DnsPayload, SslPayload, HttpPayload {}