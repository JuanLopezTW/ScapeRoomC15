package co.eci.c15.app.observabilidad;

import co.eci.c15.common.logging.EventoNegocio;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Cuando se cae un WebSocket no hay peticion ni frame, solo este evento: se abre aqui la traza y el contexto
 * para que lo que hagan sus listeners (salir de la sala, salir del mapa) quede conectado.
 */
public class DesconexionConTrazaMulticaster extends SimpleApplicationEventMulticaster {

    private static final Logger log = LoggerFactory.getLogger(DesconexionConTrazaMulticaster.class);

    private final ObjectProvider<ObservationRegistry> observaciones;

    public DesconexionConTrazaMulticaster(ObjectProvider<ObservationRegistry> observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        if (!(event instanceof SessionDisconnectEvent desconexion)) {
            super.multicastEvent(event, eventType);
            return;
        }
        ObservationRegistry registro = observaciones.getIfAvailable(() -> ObservationRegistry.NOOP);
        Observation.createNotStarted("stomp.desconexion", registro).observe(() -> {
            if (desconexion.getUser() != null) MDC.put(ContextoLog.USER_ID, desconexion.getUser().getName());
            try {
                EventoNegocio.de(log, "stomp.desconectado")
                        .con("userId", desconexion.getUser() != null ? desconexion.getUser().getName() : "desconocido")
                        .con("codigoCierre", desconexion.getCloseStatus().getCode()).info();
                super.multicastEvent(event, eventType);
            } finally {
                ContextoLog.limpiar();
            }
        });
    }
}
