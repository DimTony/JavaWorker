package com.owlite.worker;

import com.owlite.worker.listener.QueueListener;
import com.owlite.worker.processor.*;

import java.util.Map;

public class Application {
    public static void main(String[] args) {
        Map<String, JobProcessor> processors = Map.of(
            "Domain", new RetryableProcessor(new ScanJobProcessor()),
            "Repository",  new RetryableProcessor(new RepositoryJobProcessor())
        );

        QueueListener listener = new QueueListener(processors);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            listener.stop();
        }));

        listener.run();
    }
}