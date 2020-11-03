package com.example.demo.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.slf4j.Logger;

public class PrinterActor extends AbstractBehavior<PrinterActor.PrintFinalResult> {

    private final Logger logger = getContext().getLog();

    private PrinterActor(ActorContext<PrintFinalResult> context) {
        super(context);
    }

    public static Behavior<PrintFinalResult> create() {
        return Behaviors.setup(PrinterActor::new);
    }

    @Override
    public Receive<PrintFinalResult> createReceive() {
        return newReceiveBuilder()
                .onMessage(PrintFinalResult.class,
                        r -> {
                            logger.info("Received PrintFinalResult message");
                            logger.info("The text has a total number of {} words", r.totalNumberOfWords);
                            return Behaviors.stopped();
                        })
                .build();
    }

    public static class PrintFinalResult {
        Integer totalNumberOfWords;

        public PrintFinalResult(Integer totalNumberOfWords) {
            this.totalNumberOfWords = totalNumberOfWords;
        }
    }

}