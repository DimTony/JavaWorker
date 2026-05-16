package com.owlite.worker.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlite.worker.config.AppConfig;
import com.owlite.worker.config.RedisConfig;
import com.owlite.worker.model.Job;
import com.owlite.worker.processor.JobProcessor;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueListener implements Runnable {
    private final String queueName = AppConfig.get("redis.queue");
    private final int blpopTimeout = AppConfig.getInt("worker.blpop.timeout");
    private final Map<String, JobProcessor> processors;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor;
    private volatile boolean running = true;

    public QueueListener(Map<String, JobProcessor> processors) {
        this.processors = processors;
        this.executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 21
    }

    @Override
    public void run() {
        System.out.println("Listening on queue: " + queueName);
        try (Jedis jedis = RedisConfig.getPool().getResource()) {
            while (running) {
                List<String> result = jedis.blpop(blpopTimeout, queueName);
                if (result == null) continue; // timeout, loop again

                String payload = result.get(1);
                executor.submit(() -> handle(payload));
            }
        }
    }

    private void handle(String raw) {
        try {
            Job job = mapper.readValue(raw, Job.class);
            JobProcessor processor = processors.get(job.type());
            if (processor == null) {
                System.err.println("No processor for job type: " + job.type());
                return;
            }
            processor.process(job);
        } catch (Exception e) {
            System.err.println("Failed to process job: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        executor.shutdown();
    }
}