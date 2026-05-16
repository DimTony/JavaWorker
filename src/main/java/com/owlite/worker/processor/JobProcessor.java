package com.owlite.worker.processor;

import com.owlite.worker.model.Job;

public interface JobProcessor {
    void process(Job job);
}