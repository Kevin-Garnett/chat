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
public class InboundChatService extends UserParsingHandshakeHandler {//implements WebSocketHandler {

    private final ChatServicesStreams chatServicesStreams;

    public InboundChatService(ChatServicesStreams chatServicesStreams){
        this.chatServicesStreams = chatServicesStreams;
    }

    //@Override
    //public Mono<Void> handle(WebSocketSession webSocketSession) {

    @Override
    protected Mono<Void> handleInternal(WebSocketSession session) {
        return session
                .receive()
                .log(getUser(session.getId()) + "-inbound-incoming-chat-message")
                .map(WebSocketMessage::getPayloadAsText)
                .log(getUser(session.getId()) + "-inbound-convert-to-text")
                //.map(s -> session.getId() + ": " + s)
                //.log("inbound-mark-with-session-id")
                //.flatMap(this::broadcast)
                .flatMap(message -> broadcast(message, getUser(session.getId())))
                .log(getUser(session.getId()) + "-inbound-broadcast-to-broker")
                .then();
    }

    //public Mono<?> broadcast(String message){
    public Mono<?> broadcast(String message, String user){
        return Mono.fromRunnable(() -> {
           chatServicesStreams.clientToBroker().send(
                   MessageBuilder
                           .withPayload(message)
                           .setHeader(ChatServicesStreams.USER_HEADER, user)
                           .build());
        });
    }
}
