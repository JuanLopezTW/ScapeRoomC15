package co.eci.c15.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LatidosClienteInterceptorTest {

    private final LatidosClienteInterceptor interceptor = new LatidosClienteInterceptor();

    private static Message<byte[]> frame(StompCommand comando, String latidos) {
        StompHeaderAccessor stomp = StompHeaderAccessor.create(comando);
        if (latidos != null) stomp.setNativeHeader("heart-beat", latidos);
        stomp.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], stomp.getMessageHeaders());
    }

    private static long[] latidos(Message<?> m) {
        return MessageHeaderAccessor.getAccessor(m, StompHeaderAccessor.class).getHeartbeat();
    }

    @Test
    void elConnectLlegaSinLatidosDelClientePeroPidiendoLosDelServidor() {
        Message<?> salida = interceptor.preSend(frame(StompCommand.CONNECT, "10000,10000"), null);

        assertArrayEquals(new long[]{0, 10_000}, latidos(salida));
    }

    @Test
    void unConnectSinLatidosSigueSinLatidos() {
        assertArrayEquals(new long[]{0, 0}, latidos(interceptor.preSend(frame(StompCommand.CONNECT, null), null)));
    }

    @Test
    void losDemasFramesNoSeTocan() {
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, null);

        assertSame(subscribe, interceptor.preSend(subscribe, null));
    }
}
