package com.owlite.worker.engine;

import com.owlite.worker.model.EngineResult;
import com.owlite.worker.model.ScanJob;

public interface ScanEngine {
    /** Surface identifier — must match Finding.surface values: Dns | Ssl | HttpHeaders */
    String surface();
    EngineResult run(ScanJob job);
}

