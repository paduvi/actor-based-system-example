package com.example.demo;

import akka.actor.typed.ActorSystem;
import com.example.demo.actor.ReadingActor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        try {
            new SpringApplicationBuilder(DemoApplication.class).web(WebApplicationType.NONE)
                    .registerShutdownHook(true).run(args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        final String TEXT = readFromInputStream(this.getClass().getResourceAsStream("/sample-text.txt"));
        ActorSystem<ReadingActor.Command> system = ActorSystem.create(ReadingActor.create(TEXT), "printer-sample-system");

        // these are all fire and forget
        system.tell(new ReadingActor.ReadLines());
        system.tell(new ReadingActor.AsyncReadLines());

        Thread.sleep(Duration.ofSeconds(5).toMillis());
        system.terminate();
    }

    public static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
