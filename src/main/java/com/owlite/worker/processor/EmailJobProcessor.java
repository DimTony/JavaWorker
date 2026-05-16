package com.owlite.worker.processor;

import com.owlite.worker.model.Job;

public class EmailJobProcessor implements JobProcessor {
    @Override
    public void process(Job job) {
        String to = (String) job.payload().get("to");
        System.out.printf("[EmailJob] %s → sending email to %s%n", job.jobId(), to);
        // plug in your actual email logic here
    }
}