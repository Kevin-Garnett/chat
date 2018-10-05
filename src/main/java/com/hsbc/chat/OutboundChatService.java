package com.hsbc.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;


@Service
@EnableBinding(ChatServicesStreams.class)
public class OutboundChatService extends UserParsingHandshakeHandler{ //implements WebSocketHandler {

    private final static Logger log = LoggerFactory.getLogger(OutboundChatService.class);

    private Flux<Message<String>> flux;
    private FluxSink<Message<String>> chatMessageSink;

    public OutboundChatService(){
        this.flux = Flux.<Message<String>>create(
          emitter -> this.chatMessageSink = emitter, FluxSink.OverflowStrategy.IGNORE
        ).publish().autoConnect();
    }

    @StreamListener(ChatServicesStreams.BROKER_TO_CLIENT)
    public void listen(Message<String> message){
        if(chatMessageSink != null){
            log.info("Publishing " + message.getPayload() + " to websocket...");
            chatMessageSink.next(message);
        }
    }

    //@Override
    //public Mono<Void> handle(WebSocketSession webSocketSession) {

    @Override
    protected Mono<Void> handleInternal(WebSocketSession session) {
        return session
                .send(this.flux
                .filter(s -> validate(s, getUser(session.getId())))
                .map(this::transform)
                .map(session::textMessage)
                .log(getUser(session.getId()) + "-outbound-wrap-as-websocket-message"))
                .log(getUser(session.getId()) + "-outbound-publish-to-websocket");
    }

    private boolean validate(Message<String> message, String user){
        if (message.getPayload().startsWith("@")){
            String targetUser = message.getPayload().substring(1, message.getPayload().indexOf(" "));
            String sender = message.getHeaders().get(ChatServicesStreams.USER_HEADER, String.class);
            return user.equals(sender) || user.equals(targetUser);
        } else {
            return true;
        }
    }

    private String transform(Message<String> message){
        String user = message.getHeaders().get(ChatServicesStreams.USER_HEADER, String.class);
        if(message.getPayload().startsWith("@")){
            return "(" + user + "): " +message.getPayload();
        } else {
            return "(" + user + ")(all): " + message.getPayload();
        }
    }

}
