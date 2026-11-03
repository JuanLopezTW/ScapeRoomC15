package co.eci.c15.app.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Con RabbitMQ los latidos se negocian entre el navegador y el broker, no con Spring. Para mantener la
 * regla de siempre (el servidor manda latidos pero no se los exige al cliente, por las pestañas en segundo
 * plano) el CONNECT llega al broker diciendo que el cliente no manda latidos; el cliente entonces tampoco
 * los manda y el broker no lo corta por eso.
 */
public class LatidosClienteInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stomp = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (stomp == null || stomp.getCommand() != StompCommand.CONNECT || !stomp.isMutable()) return message;
        long[] latidos = stomp.getHeartbeat();
        stomp.setHeartbeat(0, latidos[1]);
        return message;
    }
}
