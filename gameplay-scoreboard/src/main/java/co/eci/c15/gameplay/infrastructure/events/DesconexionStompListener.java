package co.eci.c15.gameplay.infrastructure.events;

import co.eci.c15.gameplay.application.DesconectarJugadorUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class DesconexionStompListener {

    private final SesionJugadorRegistry sesiones;
    private final DesconectarJugadorUseCase desconectar;

    public DesconexionStompListener(SesionJugadorRegistry sesiones, DesconectarJugadorUseCase desconectar) {
        this.sesiones = sesiones;
        this.desconectar = desconectar;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sesiones.retirar(event.getSessionId())
                .ifPresent(s -> desconectar.ejecutar(s.matchId(), s.userId()));
    }
}
