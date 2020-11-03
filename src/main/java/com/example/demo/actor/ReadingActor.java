package com.example.demo.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class ReadingActor extends AbstractBehavior<ReadingActor.Command> {

    private final Logger logger = getContext().getLog();
    private final String text;

    public static Behavior<Command> create(String text) {
        return Behaviors.setup(context -> new ReadingActor(context, text));
    }

    private ReadingActor(ActorContext<Command> context, String text) {
        super(context);
        this.text = text;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadLines.class, this::onReadLines)
                .onMessage(AsyncReadLines.class, this::onAsyncReadLines)
                .onMessage(AggregatedWordCountResult.class, this::onFinalResult)
                .build();
    }

    private Behavior<Command> onReadLines(ReadLines r) {
        logger.info("Received ReadLines Command");
        String[] lines = text.split("\n+");

        List<CompletionStage<WordCounterActor.WordCountResponse>> futures = new ArrayList<>();
        for (String line : lines) {
            ActorRef<WordCounterActor.WordCountRequest> wordCountActorRef = getContext().spawnAnonymous(WordCounterActor.create());

            futures.add(AskPattern.ask(wordCountActorRef,
                    replyTo -> new WordCounterActor.WordCountRequest(line, replyTo.narrow()),
                    Duration.ofSeconds(5),
                    getContext().getSystem().scheduler()
            ));
        }

        int totalNumberOfWords = futures.stream()
                .map(CompletionStage::toCompletableFuture)
                .map(CompletableFuture::join)
                .mapToInt(response -> response.value)
                .sum();

        // forward
        ActorRef<PrinterActor.PrintFinalResult> printerActorRef = getContext().spawnAnonymous(PrinterActor.create());
        printerActorRef.tell(new PrinterActor.PrintFinalResult(totalNumberOfWords));

        return Behaviors.same();
    }

    private Behavior<Command> onAsyncReadLines(AsyncReadLines r) {
        logger.info("Received AsyncReadLines Command");
        String[] lines = text.split("\n+");

        Map<ActorRef<WordCounterActor.WordCountRequest>, String> children = new HashMap<>();
        for (String line : lines) {
            children.put(getContext().spawnAnonymous(WordCounterActor.create()), line);
        }

        Consumer<ActorRef<WordCounterActor.WordCountResponse>> sendRequests =
                replyTo -> {
                    for (Map.Entry<ActorRef<WordCounterActor.WordCountRequest>, String> entry : children.entrySet()) {
                        entry.getKey().tell(new WordCounterActor.WordCountRequest(entry.getValue(), replyTo.narrow()));
                    }
                };

        getContext().spawnAnonymous(
                AggregatorActor.create(
                        WordCounterActor.WordCountResponse.class,
                        sendRequests,
                        children.size(),
                        getContext().getSelf(),
                        this::aggregateReplies,
                        Duration.ofSeconds(5)
                ));

        return Behaviors.same();
    }

    private Behavior<Command> onFinalResult(AggregatedWordCountResult aggregatedWordCountResult) {
        int totalNumberOfWords = aggregatedWordCountResult.value;

        // forward
        ActorRef<PrinterActor.PrintFinalResult> printerActorRef = getContext().spawnAnonymous(PrinterActor.create());
        printerActorRef.tell(new PrinterActor.PrintFinalResult(totalNumberOfWords));
        return Behaviors.same();
    }

    private AggregatedWordCountResult aggregateReplies(List<WordCounterActor.WordCountResponse> replies) {
        return new AggregatedWordCountResult(replies.stream().mapToInt(r -> r.value).sum());
    }

    public interface Command {
    }

    public static class ReadLines implements Command {
    }

    public static class AsyncReadLines implements Command {
    }

    public static class AggregatedWordCountResult implements Command {
        final int value;

        public AggregatedWordCountResult(int value) {
            this.value = value;
        }
    }
}
