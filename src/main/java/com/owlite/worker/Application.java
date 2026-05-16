package com.owlite.worker;

import com.owlite.worker.listener.QueueListener;
import com.owlite.worker.processor.EmailJobProcessor;
import com.owlite.worker.processor.JobProcessor;

import java.util.Map;

public class Application {
    public static void main(String[] args) {
        Map<String, JobProcessor> processors = Map.of(
            "EMAIL", new EmailJobProcessor()
            // register more types here: "SMS", new SmsJobProcessor(), ...
        );

        QueueListener listener = new QueueListener(processors);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            listener.stop();
        }));

        listener.run(); // blocks on the BLPOP loop
    }
}