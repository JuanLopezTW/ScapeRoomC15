package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.salas.application.SalirDeSalaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;

/**
 * Un invitado que se desconecta en el lobby sale de una. El anfitrion tiene un tiempo de gracia para
 * volver, porque si sale la sala se cierra para todos: un corte de red o recargar no debe causar eso.
 */
@Component
public class DesconexionSalaListener {

    private static final Logger log = LoggerFactory.getLogger(DesconexionSalaListener.class);

    private final SesionSalaRegistry sesiones;
    private final SalirDeSalaUseCase salir;
    private final TaskScheduler scheduler;
    private final Duration gracia;

    public DesconexionSalaListener(SesionSalaRegistry sesiones, SalirDeSalaUseCase salir,
                                   @Qualifier("salasScheduler") TaskScheduler scheduler,
                                   @Value("${c15.salas.gracia-desconexion:20s}") Duration gracia) {
        this.sesiones = sesiones;
        this.salir = salir;
        this.scheduler = scheduler;
        this.gracia = gracia;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        sesiones.retirar(event.getSessionId()).ifPresent(s -> {
            if (salir.esAnfitrion(s.salaId(), s.userId())) {
                log.info("Anfitrion {} desconectado de la sala {} ({}); tiene {} para volver",
                        s.userId(), s.salaId(), event.getCloseStatus(), gracia);
                scheduler.schedule(() -> sacarSiNoVolvio(s), scheduler.getClock().instant().plus(gracia));
            } else {
                log.info("Jugador {} desconectado de la sala {} ({})", s.userId(), s.salaId(), event.getCloseStatus());
                sacarSiNoVolvio(s);
            }
        });
    }

    private void sacarSiNoVolvio(SesionSalaRegistry.Sesion s) {
        if (!sesiones.tieneOtraSesion(s.salaId(), s.userId())) salir.ejecutar(s.salaId(), s.userId());
    }
}
