package co.eci.c15.estadisticas.infrastructure.events;

import co.eci.c15.common.events.MatchFinishedEvent;
import co.eci.c15.common.events.PartidaIniciadaEvent;
import co.eci.c15.estadisticas.application.EstadisticasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Un fallo guardando estadisticas no debe impedir que la partida termine para los demas modulos. */
@Component
public class EstadisticasPartidaListener {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasPartidaListener.class);

    private final EstadisticasService estadisticas;

    public EstadisticasPartidaListener(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    @EventListener
    public void onPartidaIniciada(PartidaIniciadaEvent event) {
        estadisticas.partidaIniciada(event.matchId(), event.equipos());
    }

    @EventListener
    public void onPartidaTerminada(MatchFinishedEvent event) {
        try {
            estadisticas.partidaTerminada(event);
        } catch (RuntimeException e) {
            log.error("No se pudieron guardar las estadisticas de la partida {}", event.matchId(), e);
        }
    }
}
