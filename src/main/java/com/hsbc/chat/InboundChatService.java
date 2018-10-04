package com.hsbc.chat;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Service
@EnableBinding(ChatServicesStreams.class)
public class InboundChatService implements WebSocketHandler {

    private final ChatServicesStreams chatServicesStreams;

    public InboundChatService(ChatServicesStreams chatServicesStreams){
        this.chatServicesStreams = chatServicesStreams;
    }

    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        return webSocketSession
                .receive()
                .log("inbound-incoming-chat-message")
                .map(WebSocketMessage::getPayloadAsText)
                .log("inbound-convert-to-text")
                .map(s -> webSocketSession.getId() + ": " + s)
                .log("inbound-mark-with-session-id")
                .flatMap(this::broadcast)
                .log("inbound-broadcast-to-broker")
                .then();
    }

    public Mono<?> broadcast(String message){
        return Mono.fromRunnable(() -> {
           chatServicesStreams.clientToBroker().send(
                   MessageBuilder.withPayload(message).build());
        });
    }
}
