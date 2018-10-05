package com.hsbc.chat;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

abstract class UserParsingHandshakeHandler implements WebSocketHandler {

    private final Map<String, String> userMap;

    UserParsingHandshakeHandler(){
        this.userMap = new HashMap<>();
    }

    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        //System.out.println(webSocketSession.getHandshakeInfo().getUri());
        //System.out.println(webSocketSession.getHandshakeInfo().getUri().getQuery());

        this.userMap.put(webSocketSession.getId(),
                Stream.of(webSocketSession.getHandshakeInfo().getUri()
                .getQuery().split("&"))
                .map(s -> s.split("="))
                .filter(strings -> strings[0].equals("user"))
                .findFirst()
                .map(strings -> strings[1])
                .orElse("")
        );

        return handleInternal(webSocketSession);
    }

    abstract protected Mono<Void> handleInternal(WebSocketSession session);

    String getUser(String id){
        return userMap.get(id);
    }
}
