package com.owlite.worker.processor;

import com.owlite.worker.model.ScanJob;

public interface JobProcessor {
    void process(ScanJob job);
}