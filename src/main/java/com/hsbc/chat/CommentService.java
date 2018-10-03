package com.hsbc.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@Service
@EnableBinding(Sink.class)
public class CommentService implements WebSocketHandler {

    private final static Logger log = LoggerFactory.getLogger(CommentService.class);

    private ObjectMapper mapper;
    private Flux<Comment> flux;
    private FluxSink<Comment> webSocketCommentSink;

    CommentService(ObjectMapper mapper){
        this.mapper = mapper;
        this.flux = Flux.<Comment>create(
                emitter -> this.webSocketCommentSink = emitter,
                FluxSink.OverflowStrategy.IGNORE
        ).publish().autoConnect();
    }

    @StreamListener(Sink.INPUT)
    public void broadcast(Comment comment){
        if (webSocketCommentSink != null){
            log.info("Publishing " + comment.toString() + " to websocket...");
            webSocketCommentSink.next(comment);
        }
    }

    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        return webSocketSession.send(
                this.flux
                .map(comment -> {
                    try{
                        return mapper.writeValueAsString(comment);
                    }catch(JsonProcessingException e){
                        throw new RuntimeException(e);
                    }
                })
                .log("encode-as-json")
                .map(webSocketSession::textMessage)
                .log("wrap-as-websocket-message")
        )
        .log("publish-to-websocket");
    }

}
