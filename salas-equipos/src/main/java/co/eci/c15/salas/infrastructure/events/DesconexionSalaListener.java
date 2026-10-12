package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.SalirDeSalaUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Quien cierra la pagina en el lobby sale de la sala; si era el anfitrion, la sala se cierra. */
@Component
public class DesconexionSalaListener {

    private final SesionSalaRegistry sesiones;
    private final SalirDeSalaUseCase salir;

    public DesconexionSalaListener(SesionSalaRegistry sesiones, SalirDeSalaUseCase salir) {
        this.sesiones = sesiones;
        this.salir = salir;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sesiones.retirar(event.getSessionId())
                .filter(s -> !sesiones.tieneOtraSesion(s.salaId(), s.userId()))
                .ifPresent(s -> salir.ejecutar(s.salaId(), s.userId()));
    }
}
