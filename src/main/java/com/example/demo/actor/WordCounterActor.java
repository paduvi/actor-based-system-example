package com.example.demo.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public class WordCounterActor {

    public static Behavior<WordCountRequest> create() {
        return Behaviors
                .receive(WordCountRequest.class)
                .onMessage(WordCountRequest.class, request -> {
                    int numberOfWords = countWordsFromLine(request.line);
                    request.replyTo.tell(new WordCountResponse(numberOfWords));
                    return Behaviors.same();
                })
                .build();
    }

    private static int countWordsFromLine(String line) {
        return line.split("\\s+").length;
    }

    public static class WordCountRequest {
        final String line;
        final ActorRef<WordCountResponse> replyTo;

        public WordCountRequest(String line, ActorRef<WordCountResponse> replyTo) {
            this.line = line;
            this.replyTo = replyTo;
        }
    }

    public static class WordCountResponse {
        final int value;

        public WordCountResponse(int value) {
            this.value = value;
        }
    }

}
