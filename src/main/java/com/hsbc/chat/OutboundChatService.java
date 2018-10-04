package com.hsbc.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;


@Service
@EnableBinding(ChatServicesStreams.class)
public class OutboundChatService implements WebSocketHandler {

    private final static Logger log = LoggerFactory.getLogger(OutboundChatService.class);

    private Flux<String> flux;
    private FluxSink<String> chatMessageSink;

    public OutboundChatService(){
        this.flux = Flux.<String>create(
          emitter -> this.chatMessageSink = emitter, FluxSink.OverflowStrategy.IGNORE
        ).publish().autoConnect();
    }

    @StreamListener(ChatServicesStreams.BROKER_TO_CLIENT)
    public void listen(String message){
        if(chatMessageSink != null){
            log.info("Publishing " + message + " to websocket...");
            chatMessageSink.next(message);
        }
    }

    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        return webSocketSession
                .send(this.flux
                .map(webSocketSession::textMessage)
                .log("outbound-wrap-as-websocket-message"))
                .log("outbound-publish-to-websocket");
    }

}
