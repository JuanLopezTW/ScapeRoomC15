package co.eci.c15.app.observabilidad;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Cada frame STOMP del cliente (CONNECT, SUBSCRIBE, SEND...) tiene su traza y su contexto de log, como una
 * peticion HTTP. Va antes de StompSeguridadInterceptor para cubrir tambien sus rechazos. preSend y
 * afterSendCompletion corren en el mismo hilo. Los latidos no traen comando y se ignoran.
 */
@Component
public class StompTrazaInterceptor implements ChannelInterceptor {

    private record EnCurso(Observation observacion, Observation.Scope alcance) {
    }

    private final ObservationRegistry observaciones;
    private final ThreadLocal<EnCurso> enCurso = new ThreadLocal<>();

    public StompTrazaInterceptor(ObservationRegistry observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stomp = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        StompCommand comando = stomp != null ? stomp.getCommand() : null;
        if (comando == null) return message;

        Observation observacion = Observation.start("stomp." + comando.name().toLowerCase(Locale.ROOT), observaciones);
        enCurso.set(new EnCurso(observacion, observacion.openScope()));
        if (stomp.getUser() != null) MDC.put(ContextoLog.USER_ID, stomp.getUser().getName());
        ContextoLog.ponerRecurso(stomp.getDestination());
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        EnCurso actual = enCurso.get();
        if (actual == null) return;
        enCurso.remove();
        ContextoLog.limpiar();
        if (ex != null) actual.observacion().error(ex);
        actual.alcance().close();
        actual.observacion().stop();
    }
}
