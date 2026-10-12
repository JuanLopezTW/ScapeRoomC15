package co.eci.c15.salas.infrastructure.events;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.salas.application.FinalizarPartidaUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al terminar la partida (HU-67.6) la sala sale del catalogo. */
@Component
public class PartidaTerminadaListener {

    private final FinalizarPartidaUseCase finalizar;

    public PartidaTerminadaListener(FinalizarPartidaUseCase finalizar) {
        this.finalizar = finalizar;
    }

    @EventListener
    public void onPartidaTerminada(MatchFinishedEvent event) {
        finalizar.ejecutar(event.matchId());
    }
}
